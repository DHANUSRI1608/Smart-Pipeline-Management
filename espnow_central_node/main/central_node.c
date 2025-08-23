#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_now.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_http_client.h"
#include "esp_netif.h"
#include "cJSON.h"

#define WIFI_SSID      "dinesh"
#define WIFI_PASS      "dinesh12"
#define SERVER_URL     "http://10.17.195.235:8080/api/sensor/espnow"
 
static const char *TAG = "ESP_NOW_CENTRAL";

/* -------------------- WIFI -------------------- */
static void wifi_event_handler(void* arg, esp_event_base_t event_base,
                               int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        ESP_LOGI(TAG, "Disconnected. Reconnecting...");
        esp_wifi_connect();
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "Got IP: " IPSTR, IP2STR(&event->ip_info.ip));
    }
}

void wifi_init_sta(void) {
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));

    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT,
                                                        ESP_EVENT_ANY_ID,
                                                        &wifi_event_handler,
                                                        NULL,
                                                        NULL));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT,
                                                        IP_EVENT_STA_GOT_IP,
                                                        &wifi_event_handler,
                                                        NULL,
                                                        NULL));

    wifi_config_t wifi_config = {
        .sta = {
            .ssid = WIFI_SSID,
            .password = WIFI_PASS,
            .threshold.authmode = WIFI_AUTH_WPA2_PSK,
        },
    };
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "wifi_init_sta finished.");
}

/* -------------------- HTTP -------------------- */
void send_data_to_server(const char *json_data) {
    ESP_LOGI(TAG, "Sending JSON to server: %s", json_data);

    esp_http_client_config_t config = {
        .url = SERVER_URL,
        .method = HTTP_METHOD_POST,
        .timeout_ms = 10000  // 10 second timeout
    };
    
    esp_http_client_handle_t client = esp_http_client_init(&config);
    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, json_data, strlen(json_data));

    esp_err_t err = esp_http_client_perform(client);
    
    if (err == ESP_OK) {
        int status_code = esp_http_client_get_status_code(client);
        if (status_code == 200) {
            ESP_LOGI(TAG, "Data sent successfully. Status: %d", status_code);
        } else {
            ESP_LOGE(TAG, "Server error. Status: %d", status_code);
            
            // Read response body for error details
            int content_length = esp_http_client_get_content_length(client);
            if (content_length > 0) {
                char *response = malloc(content_length + 1);
                int read_len = esp_http_client_read(client, response, content_length);
                response[read_len] = '\0';
                ESP_LOGE(TAG, "Server response: %s", response);
                free(response);
            }
        }
    } else {
        ESP_LOGE(TAG, "HTTP request failed: %s", esp_err_to_name(err));
    }
    
    esp_http_client_cleanup(client);
}

/* -------------------- ESPNOW -------------------- */
static void espnow_recv_cb(const esp_now_recv_info_t *recv_info,
                           const uint8_t *data, int len) {
    if (len > 0 && len < 200) { // Increased buffer for JSON
        char received_data[200] = {0};
        memcpy(received_data, data, len);
        received_data[len] = '\0';

        ESP_LOGI(TAG, "Received from ESPNOW: %s", received_data);

        // Parse JSON to validate structure
        cJSON *root = cJSON_Parse(received_data);
        if (root != NULL) {
            // Check if it has the expected fields
            if (cJSON_GetObjectItem(root, "node_id") && 
                cJSON_GetObjectItem(root, "temperature") &&
                cJSON_GetObjectItem(root, "humidity")) {
                
                ESP_LOGI(TAG, "Valid JSON received, sending to server");
                send_data_to_server(received_data);
            } else {
                ESP_LOGE(TAG, "JSON missing required fields");
            }
            cJSON_Delete(root);
        } else {
            ESP_LOGE(TAG, "Invalid JSON received");
        }
    } else {
        ESP_LOGW(TAG, "Unexpected data length: %d", len);
    }
}

void espnow_init(void) {
    ESP_ERROR_CHECK(esp_now_init());
    
    // Add broadcast peer for ESPNOW
    esp_now_peer_info_t peer_info = {
        .peer_addr = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF}, // Broadcast address
        .channel = 1,
        .ifidx = WIFI_IF_STA,
        .encrypt = false
    };
    ESP_ERROR_CHECK(esp_now_add_peer(&peer_info));
    
    ESP_ERROR_CHECK(esp_now_register_recv_cb(espnow_recv_cb));
    ESP_LOGI(TAG, "ESP-NOW initialized");
}

/* -------------------- MAIN -------------------- */
void app_main(void) {
    // Initialize NVS
    ESP_ERROR_CHECK(nvs_flash_init());

    // Connect WiFi
    wifi_init_sta();

    // Wait for WiFi connection
    ESP_LOGI(TAG, "Waiting for WiFi connection...");
    vTaskDelay(10000 / portTICK_PERIOD_MS); // Increased delay

    // Check WiFi connection status
    wifi_ap_record_t ap_info;
    if (esp_wifi_sta_get_ap_info(&ap_info) == ESP_OK) {
        ESP_LOGI(TAG, "Connected to SSID: %s, RSSI: %d", ap_info.ssid, ap_info.rssi);
    } else {
        ESP_LOGE(TAG, "Not connected to WiFi");
    }

    // Init ESPNOW
    espnow_init();

    ESP_LOGI(TAG, "ESP-NOW Central ready. Waiting for data...");

    // Keep the task alive
    while (1) {
        vTaskDelay(10000 / portTICK_PERIOD_MS);
        ESP_LOGI(TAG, "Still running...");
    }
}
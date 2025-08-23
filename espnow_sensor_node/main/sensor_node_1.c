#include <stdio.h>
#include <string.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_system.h"
#include "esp_random.h"
#include "esp_event.h"
#include "esp_wifi.h"
#include "esp_now.h"
#include "nvs_flash.h"
#include "cJSON.h"

#define TAG "ESP_NOW_SENDER"

// ---------- Callback when data is sent ----------
static void on_data_sent(const uint8_t *mac_addr, esp_now_send_status_t status) {
    ESP_LOGI(TAG, "Send Status: %s", status == ESP_NOW_SEND_SUCCESS ? "Success" : "Fail");
}

// ---------- ESP-NOW Init ----------
static void espnow_init(void) {
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_start());
    ESP_ERROR_CHECK(esp_wifi_set_channel(1, WIFI_SECOND_CHAN_NONE));

    ESP_ERROR_CHECK(esp_now_init());
    ESP_ERROR_CHECK(esp_now_register_send_cb(on_data_sent));

    // ✅ Add central node as peer
    esp_now_peer_info_t peerInfo = {0};
    uint8_t central_mac[] = {0xC0, 0x5D, 0x89, 0xB1, 0xAA, 0xC8}; // Replace with your central MAC
    memcpy(peerInfo.peer_addr, central_mac, 6);
    peerInfo.channel = 1;   // Must match central’s channel
    peerInfo.encrypt = false;

    if (!esp_now_is_peer_exist(peerInfo.peer_addr)) {
        ESP_ERROR_CHECK(esp_now_add_peer(&peerInfo));
    }
}

// ---------- Sensor Node Send Task ----------
static void send_task(void *pvParameter) {
    while (1) {
        // Simulated sensor readings
        float temperature = 25.0 + (esp_random() % 100) / 10.0;
        float humidity = 50.0 + (esp_random() % 100) / 10.0;
        float pressure = 1000.0 + (esp_random() % 50);
        float light = (esp_random() % 1000) / 10.0;

        // Create JSON
        cJSON *root = cJSON_CreateObject();
        cJSON_AddStringToObject(root, "node_id", "Node 1");
        cJSON_AddNumberToObject(root, "temperature", temperature);
        cJSON_AddNumberToObject(root, "humidity", humidity);
        cJSON_AddNumberToObject(root, "pressure", pressure);
        cJSON_AddNumberToObject(root, "light", light);

        char *json_str = cJSON_PrintUnformatted(root);
        ESP_LOGI(TAG, "Data sent: %s", json_str);

        // ✅ Send only to central
        uint8_t central_mac[] = {0xC0, 0x5D, 0x89, 0xB1, 0xAA, 0xC8};
        esp_err_t result = esp_now_send(central_mac, (uint8_t *)json_str, strlen(json_str)+1);

        if (result != ESP_OK) {
            ESP_LOGE(TAG, "Send Error: %d", result);
        }

        cJSON_Delete(root);
        free(json_str);

        vTaskDelay(pdMS_TO_TICKS(5000)); // Send every 5s
    }
}

// ---------- Main ----------
void app_main(void) {
    ESP_ERROR_CHECK(nvs_flash_init());
    ESP_LOGI(TAG, "espnow [version: 1.0] init");

    espnow_init();
    xTaskCreate(send_task, "send_task", 4096, NULL, 5, NULL);
}

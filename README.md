## System Architecture

Pressure Sensor ─┐
                 ├──> ESP32 ───> ESP32NOW ───> Cloud Dashboard
Flow Sensor ─────┘                    │
                                     │
                                Alert System
                             (Notification)

## Working Principle
1. Sensors collect pipeline data  
2. ESP32 reads sensor values  
3. Data is processed inside ESP32  
4. Values are sent to cloud via WiFi  
5. Dashboard displays real-time monitoring  
6. Alert triggered when abnormal condition occurs 

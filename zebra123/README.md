# Zebra123 - Zebra API3 SDK + Datawedge Plugin

The Zebra123 plugin integrates both the RFID API3 library and DataWedge into a single package. 

The plugin is designed to work with both rfid and barcode scan enabled devices and provides seamless integration of both the datawedge and the RFID API3 library.

If the device is RFID enabled, the SDK library is used, otherwise the plugin uses datawedge for barcode scanning. Since RSSI is NOT supported under datawedge, this plugin is useful if RSSI is required.

You can specfify the connection method using connect(method: dataWedge), connect(method: zebraSdk) or connect(). If no method is specified, the plugin will use the sdk (if running on an rfid enabled device) or datawedge.

## Installing
1. Copy [RFIDAPI3Library](https://github.com/AppDaddy-Software-Solutions-Inc/Flutter-Markup-Language/tree/main/example/android/RFIDAPI3Library) and its contents to your `android` project folder.

2. In `settings.gradle` in the `android` folder, add the following lines to the bottom of the file:

   ```javascript
    include ":app", ':RFIDAPI3Library'
    project(":RFIDAPI3Library").projectDir = file("./RFIDAPI3Library")
   ```

3. In `build.gradle` in the `android/app` folder, add this to the bottom of the file:

    ```javascript
    dependencies {
    implementation project(":RFIDAPI3Library",)
    }   
    ```

4. In `AndroidManifest.xml` in the `android/app/src/main` folder,

   Add `xmlns:tools` directive to the top level manifest tag
   ```xml
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package= ... >
    ```

   Add the `tools` directive to the application tag
   ```xml
    <application
        android:name="${applicationName}"
        tools:replace="android:label"
        ...
        >
    ```

7. In `android/app/build.gradle`, make sure the minSdkVersion is 19 or higher


## Example

```dart
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:zebra123/zebra123.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  Zebra123? zebra123;

  @override
  void initState() {
    zebra123 = Zebra123(callback: callback);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    var connectBtn = OutlinedButton(onPressed: () => zebra123?.connect(), child: const Text("Connect"));
    var disconnectBtn = OutlinedButton(onPressed: () => zebra123?.disconnect(), child: const Text("Disconnect"));

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Zebra123 Plugin Example'),
        ),
        body: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [connectBtn, disconnectBtn]),
        ),
      ),
    );
  }

  void callback(ZebraInterfaces source, ZebraEvents event, dynamic data) {

    switch (event) {

      case ZebraEvents.readBarcode:
        if (data is List<Barcode>) {
          for (Barcode barcode in data) {
            if (kDebugMode) print("Source: $source Barcode: ${barcode.barcode} Format: ${barcode.format} Date: ${barcode.seen}");
          }
        }
        break;

      case ZebraEvents.readRfid:
        if (data is List<RfidTag>) {
          for (RfidTag tag in data) {
            if (kDebugMode) print("Source: $source Tag: ${tag.id} Rssi: ${tag.rssi}");
          }
        }
        break;

      case ZebraEvents.error:
        if (data is Error) {
          if (kDebugMode) print("Source: $source Error: ${data.message}");
        }
        break;

      case ZebraEvents.connectionStatus:
        if (data is ConnectionStatus) {
          if (kDebugMode) print("Source: $source ConnectionStatus: ${data.status}");
        }
        break;

      default:
        if (kDebugMode) {
          if (kDebugMode) print("Source: $source Unknown Event: $event");
        }
    }
  }
}
```
# Zebra123 - Zebra API3 SDK + Datawedge Plugin

The Zebra123 plugin integrates both the RFID API3 library and DataWedge into a single package. 

The plugin is designed to work with both rfid and barcode scan enabled devices and provides seamless integration of both the datawedge and the RFID API3 library.

If the device is RFID enabled, the SDK library is used, otherwise the plugin uses datawedge for barcode scanning. Since RSSI is NOT supported under datawedge, this plugin is useful if RSSI is required.

You can specfify the connection method using connect(method: dataWedge), connect(method: zebraSdk) or connect(). If no method is specified, the plugin will use the sdk (if running on an rfid enabled device) or datawedge.

## Installing
1. Copy [RFIDAPI3Library](https://github.com/AppDaddy-Software-Solutions-Inc/zebra123/tree/master/zebra123/android/RFIDAPI3Library) and its contents to your `android` project folder.

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

4. In `build.gradle` in the `android/app` folder, add to the `release` segnment:

    ```javascript
        release {

            // necessary for zebra sdk in release mode
            minifyEnabled false
            shrinkResources false
        }
   ```
   This is a temporay 'hack' until I can find a way to properly include the zebra sdk in release mode.
   Note: This only needs to be done if you wish to use RFID via the SDK. This is not necessary for DataWedge.

5. In `AndroidManifest.xml` in the `android/app/src/main` folder,

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

6. In `android/app/build.gradle`, make sure the minSdkVersion is 19 or higher


## Example

```dart
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:zebra123/zebra123.dart';

void main() {
  runApp(const MyApp());
}

enum Views { list, write }

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  Zebra123? zebra123;
  ZebraInterfaces interface = ZebraInterfaces.unknown;
  ZebraConnectionStatus connectionStatus = ZebraConnectionStatus.disconnected;
  List<Barcode> barcodes = [];

  List<RfidTag> tags = [];
  RfidTag? tag;

  bool scanning = false;
  bool tracking = false;

  Views view = Views.list;

  @override
  void initState() {
    zebra123 = Zebra123(callback: callback);
    super.initState();
  }

  void startScanning() {
    zebra123?.startScanning();
    setState(() {
      scanning = true;
      tracking = false;
    });
  }

  void stopScanning() {
    zebra123?.stopScanning();
    setState(() {
      scanning = false;
      tracking = false;
    });
  }

  void startTracking(List<String> tags) {
    zebra123?.startTracking(tags);
    setState(() {
      scanning = false;
      tracking = true;
    });
  }

  void stopTracking() {
    zebra123?.stopTracking();
    setState(() {
      scanning = false;
      tracking = false;
    });
  }

  void stop() {
    if (scanning) stopScanning();
    if (tracking) stopTracking();
  }

  Widget _listView() {
    List<Widget> children = [];

    var pad = const Padding(padding: EdgeInsets.only(left: 10));

    Widget connectBtn;
    if (zebra123?.connectionStatus == ZebraConnectionStatus.connected) {
      connectBtn = FloatingActionButton(
          backgroundColor: Colors.lightGreenAccent,
          onPressed: () => zebra123?.disconnect(),
          child: const Text("Disconnect",
              style: TextStyle(color: Colors.black, fontSize: 16)));
    } else {
      connectBtn = FloatingActionButton(
          backgroundColor: Colors.redAccent.shade100,
          onPressed: () => zebra123?.connect(),
          child: const Text("Connect",
              style: TextStyle(color: Colors.black, fontSize: 16)));
    }
    connectBtn = Padding(
        padding: const EdgeInsets.only(left: 5, right: 5),
        child: SizedBox(width: 100, height: 50, child: connectBtn));

    Widget scanBtn = const Offstage();
    if (interface == ZebraInterfaces.rfidapi3 &&
        zebra123?.connectionStatus == ZebraConnectionStatus.connected &&
        !scanning &&
        !tracking) {
      scanBtn = FloatingActionButton(
          backgroundColor: Colors.lightGreenAccent,
          onPressed: () => startScanning(),
          child: const Text("Scan",
              style: TextStyle(color: Colors.black, fontSize: 16)));
      scanBtn = Padding(
          padding: const EdgeInsets.only(left: 5, right: 5),
          child: SizedBox(width: 75, height: 50, child: scanBtn));
    }

    Widget stopBtn = const Offstage();
    if (interface == ZebraInterfaces.rfidapi3 && (scanning || tracking)) {
      stopBtn = FloatingActionButton(
          backgroundColor: Colors.redAccent.shade100,
          onPressed: () => stop(),
          child: const Text("Stop",
              style: TextStyle(color: Colors.black, fontSize: 16)));
      stopBtn = Padding(
          padding: const EdgeInsets.only(left: 5, right: 5),
          child: SizedBox(width: 75, height: 50, child: stopBtn));
    }

    var buttons = Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [connectBtn, scanBtn, stopBtn]);
    children.add(buttons);

    List<Widget> results = [];
    for (var barcode in barcodes) {
      var t1 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Barcode:"),
        pad,
        Text(barcode.barcode,
            style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t2 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Format:"),
        pad,
        Text(barcode.format,
            style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t3 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Seen:"),
        pad,
        Text(barcode.seen, style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t4 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Interface:"),
        pad,
        Text("${barcode.interface}",
            style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var subtitle = Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [t1, t2, t3, t4]);
      results.add(ListTile(
          leading: const Icon(Icons.barcode_reader),
          subtitle: SingleChildScrollView(
              scrollDirection: Axis.horizontal, child: subtitle)));
    }
    for (var tag in tags) {
      var t1 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Tag:"),
        pad,
        Text(tag.epc, style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t2 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Rssi:"),
        pad,
        Text("${tag.rssi}", style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t3 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Seen:"),
        pad,
        Text(tag.seen, style: const TextStyle(fontWeight: FontWeight.bold))
      ]);
      var t4 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
        const Text("Interface:"),
        pad,
        Text("${tag.interface}",
            style: const TextStyle(fontWeight: FontWeight.bold))
      ]);

      Widget writeBtn = OutlinedButton(
          child: const Text("Write",
              style: TextStyle(color: Colors.black, fontSize: 16)),
          onPressed: () {
            setState(() {
              this.tag = tag;
              view = Views.write;
            });
          });
      writeBtn = SizedBox(width: 100, height: 35, child: writeBtn);

      Widget trackBtn = OutlinedButton(
          onPressed: () => _trackTag(tag.epc),
          child: const Text("Track",
              style: TextStyle(color: Colors.black, fontSize: 16)));
      trackBtn = SizedBox(width: 100, height: 35, child: trackBtn);

      Widget t5 = Padding(
          padding: EdgeInsets.only(top: 10),
          child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [writeBtn, pad, trackBtn]));
      if (tracking) {
        t5 = Offstage();
      }

      var subtitle = Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [t1, t2, t3, t4, t5]);

      results.add(ListTile(
          leading: const Icon(Icons.barcode_reader),
          subtitle: SingleChildScrollView(
              scrollDirection: Axis.horizontal, child: subtitle)));
    }
    children.addAll(results);

    return Column(mainAxisSize: MainAxisSize.min, children: children);
  }

  void _writeTag() {
    var epc = tag?.epc ?? "";
    var epcNew = tag?.epcNew ?? "";
    var data = tag?.memoryBankData ?? "";
    var password = double.tryParse(tag?.password ?? "");
    var passwordNew = double.tryParse(tag?.passwordNew ?? tag?.password ?? "");
    zebra123?.writeTag(epc,
        epcNew: epcNew,
        password: password,
        passwordNew: passwordNew,
        data: data);
  }

  void _trackTag(String epc) {
    startTracking([epc]);
  }

  Widget _writeView() {
    List<Widget> children = [];

    var pad = const Padding(padding: EdgeInsets.only(left: 10));

    Widget quitBtn = FloatingActionButton(
        backgroundColor: Colors.lightGreenAccent,
        onPressed: () {
          setState(() {
            view = Views.list;
          });
        },
        child: const Text("Quit",
            style: TextStyle(color: Colors.black, fontSize: 16)));
    quitBtn = SizedBox(width: 75, height: 50, child: quitBtn);

    Widget writeBtn = FloatingActionButton(
        backgroundColor: Colors.lightGreenAccent,
        onPressed: () => _writeTag(),
        child: const Text("Write",
            style: TextStyle(color: Colors.black, fontSize: 16)));
    writeBtn = SizedBox(width: 75, height: 50, child: writeBtn);

    var buttons = Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [writeBtn, pad, quitBtn]);
    children.add(buttons);

    var t1 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
      const Text("ID (old):"),
      pad,
      SizedBox(
          width: 250,
          height: 50,
          child: Text(tag?.epc ?? "",
              style: const TextStyle(fontWeight: FontWeight.bold)))
    ]);
    children.add(t1);

    var c2 = TextEditingController(text: tag?.epc ?? "");
    var t2 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
      const Text("ID (new):"),
      pad,
      SizedBox(
          width: 250,
          height: 50,
          child: TextField(
              controller: c2,
              onChanged: (value) => tag?.epcNew = value,
              style: const TextStyle(fontWeight: FontWeight.bold)))
    ]);
    children.add(t2);

    var c3 = TextEditingController(text: tag?.epc ?? "");
    var t3 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
      const Text("Data:"),
      pad,
      SizedBox(
          width: 250,
          height: 50,
          child: TextField(
              controller: c3,
              onChanged: (value) => tag?.memoryBankData = value,
              style: const TextStyle(fontWeight: FontWeight.bold)))
    ]);
    children.add(t3);

    var c4 = TextEditingController(text: "");
    var t4 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [
      const Text("Password:"),
      pad,
      SizedBox(
          width: 250,
          height: 50,
          child: TextField(
              controller: c4,
              onChanged: (value) => tag?.password = value,
              style: const TextStyle(fontWeight: FontWeight.bold)))
    ]);
    children.add(t4);

    return Column(mainAxisSize: MainAxisSize.min, children: children);
  }

  @override
  Widget build(BuildContext context) {
    Widget child = view == Views.write ? _writeView() : _listView();

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Zebra123 Plugin Example'),
        ),
        body:
            SingleChildScrollView(scrollDirection: Axis.vertical, child: child),
      ),
    );
  }

  void callback(ZebraInterfaces interface, Events event, dynamic data) {
    this.interface = interface;

    switch (event) {
      case Events.readBarcode:
        barcodes.clear();
        if (data is List<Barcode>) {
          for (Barcode barcode in data) {
            barcodes.add(barcode);
            if (kDebugMode) {
              print(
                  "Barcode: ${barcode.barcode} Format: ${barcode.format} Seen: ${barcode.seen} Interface: ${barcode.interface} ");
            }
          }
        }
        setState(() {});
        break;

      case Events.readRfid:
        tags.clear();
        if (data is List<RfidTag>) {
          for (RfidTag tag in data) {
            tags.add(tag);
            if (kDebugMode) {
              print(
                  "Tag: ${tag.epc} Rssi: ${tag.rssi}  Seen: ${tag.seen} Interface: ${tag.interface}");
            }
          }
        }
        setState(() {});
        break;

      case Events.error:
        if (data is Error) {
          if (kDebugMode) print("Interface: $interface Error: ${data.message}");
        }
        break;

      case Events.connectionStatus:
        if (data is ConnectionStatus) {
          if (kDebugMode) {
            print("Interface: $interface ConnectionStatus: ${data.status}");
          }
        }
        if (data.status != connectionStatus) {
          setState(() {
            connectionStatus = data.status;
          });
        }
        break;

      default:
        if (kDebugMode) {
          if (kDebugMode) print("Interface: $interface Unknown Event: $event");
        }
    }
  }
}
```
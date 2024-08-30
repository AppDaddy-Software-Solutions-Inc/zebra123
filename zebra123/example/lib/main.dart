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
    var barcodeBtn = OutlinedButton(onPressed: () => zebra123?.setMode("barcode"), child: const Text("Barcode Mode"));
    var rfidBtn = OutlinedButton(onPressed: () => zebra123?.setMode("rfid"), child: const Text("RFID Mode"));

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Zebra123 Plugin EXample'),
        ),
        body: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [connectBtn, barcodeBtn, rfidBtn]),
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

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
    zebra123 = Zebra123(
      readCallback: readCallback,
      connectionCallback: connectionCallback,
      errorCallback: errorCallback,
    );
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

  void errorCallback(ErrorResult error) {
    if (kDebugMode) {
      print("Error: ${error.errorMessage}");
    }
  }

  void readCallback(List<TagData> data) {
    if (kDebugMode) {
      for (TagData tag in data) {
        print("Tag: ${tag.id} Rssi: ${tag.rssi}");
      }
    }
  }

  void connectionCallback(ConnectionStatus status) {
    if (kDebugMode) {
        print("Connection Status: $status");
    }
  }
}

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
  ZebraInterfaces interface = ZebraInterfaces.unknown;
  ZebraConnectionStatus connectionStatus = ZebraConnectionStatus.disconnected;
  List<Barcode> barcodes = [];
  List<RfidTag> tags = [];
  bool scanning = false;

  @override
  void initState() {
    zebra123 = Zebra123(callback: callback);
    super.initState();
  }

  void startScan() {
    zebra123?.scan(ZebraScanRequest.rfidStartInventory);
    setState(() {
      scanning = true;
    });
  }

  void stopScan() {
    zebra123?.scan(ZebraScanRequest.rfidStopInventory);
    setState(() {
      scanning = false;
    });
  }

  @override
  Widget build(BuildContext context) {
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
    connectBtn = SizedBox(width: 150, height: 50, child: connectBtn);

    Widget scanBtn = const Offstage();
    if (interface == ZebraInterfaces.rfidapi3 &&
        zebra123?.connectionStatus == ZebraConnectionStatus.connected) {
      scanBtn = scanning
          ? FloatingActionButton(
              backgroundColor: Colors.lightGreenAccent,
              onPressed: () => stopScan(),
              child: const Text("Stop Scan",
                  style: TextStyle(color: Colors.black, fontSize: 16)))
          : FloatingActionButton(
              backgroundColor: Colors.lightGreenAccent,
              onPressed: () => startScan(),
              child: const Text("Start Scan",
                  style: TextStyle(color: Colors.black, fontSize: 16)));
      scanBtn = SizedBox(width: 150, height: 50, child: scanBtn);
    }

    var buttons = Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [connectBtn, pad, scanBtn]);
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
        Text(tag.id, style: const TextStyle(fontWeight: FontWeight.bold))
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
      var subtitle = Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [t1, t2, t3, t4]);
      results.add(ListTile(
          leading: const Icon(Icons.barcode_reader),
          subtitle: SingleChildScrollView(
              scrollDirection: Axis.horizontal, child: subtitle)));
    }
    children.addAll(results);

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Zebra123 Plugin Example'),
        ),
        body: SingleChildScrollView(
            scrollDirection: Axis.vertical,
            child: Column(mainAxisSize: MainAxisSize.min, children: children)),
      ),
    );
  }

  void callback(ZebraInterfaces interface, ZebraEvents event, dynamic data) {
    this.interface = interface;

    switch (event) {
      case ZebraEvents.readBarcode:
        barcodes.clear();
        if (data is List<Barcode>) {
          for (Barcode barcode in data) {
            barcodes.add(barcode);
            if (kDebugMode)
              print(
                  "Barcode: ${barcode.barcode} Format: ${barcode.format} Seen: ${barcode.seen} Interface: ${barcode.interface} ");
          }
        }
        setState(() {});
        break;

      case ZebraEvents.readRfid:
        tags.clear();
        if (data is List<RfidTag>) {
          for (RfidTag tag in data) {
            tags.add(tag);
            if (kDebugMode)
              print(
                  "Tag: ${tag.id} Rssi: ${tag.rssi}  Seen: ${tag.seen} Interface: ${tag.interface}");
          }
        }
        setState(() {});
        break;

      case ZebraEvents.error:
        if (data is Error) {
          if (kDebugMode) print("Interface: $interface Error: ${data.message}");
        }
        break;

      case ZebraEvents.connectionStatus:
        if (data is ConnectionStatus) {
          if (kDebugMode)
            print("Interface: $interface ConnectionStatus: ${data.status}");
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

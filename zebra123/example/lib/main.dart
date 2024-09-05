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
  ZebraConnectionStatus connectionStatus = ZebraConnectionStatus.disconnected;
  List<Barcode> barcodes = [];
  List<RfidTag> tags = [];

  @override
  void initState() {
    zebra123 = Zebra123(callback: callback);
    super.initState();
  }

  @override
  Widget build(BuildContext context) {

    List<Widget> children = [];

    Widget connectBtn;
    if (zebra123?.connectionStatus == ZebraConnectionStatus.connected) {
      connectBtn = FloatingActionButton(backgroundColor: Colors.lightGreenAccent, onPressed: () => zebra123?.disconnect(), child: Text("Disconnect", style: TextStyle(color: Colors.black, fontSize: 20)));
    } else {
      connectBtn = FloatingActionButton(backgroundColor: Colors.redAccent.shade100, onPressed: () => zebra123?.connect(), child: Text("Connect", style: TextStyle(color: Colors.black, fontSize: 20)));
    }
    connectBtn = Row(mainAxisAlignment: MainAxisAlignment.center, children: [SizedBox(width: 200, height: 50, child: connectBtn)]);
    children.add(connectBtn);

    var pad = const Padding(padding: EdgeInsets.only(left:10));

    List<Widget> results = [];
    for (var barcode in this.barcodes) {
      var t1 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Barcode:"), pad, Text("${barcode.barcode}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var t2 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Format:"), pad, Text("${barcode.format}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var t3 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Seen:"), pad, Text("${barcode.seen}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var t4 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Interface:"), pad, Text("${barcode.interface}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var subtitle = Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [t1,t2,t3,t4]);
      results.add(ListTile(leading: Icon(Icons.barcode_reader), subtitle: SingleChildScrollView(scrollDirection: Axis.horizontal, child: subtitle)));
    }
    for (var tag in this.tags) {
      var t1 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Tag:"), pad, Text("${tag.id}"  , style: TextStyle(fontWeight: FontWeight.bold))]);
      var t2 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Rssi:"), pad, Text("${tag.rssi}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var t3 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Seen:"), pad, Text("${tag.seen}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var t4 = Row(mainAxisAlignment: MainAxisAlignment.start, children: [Text("Interface:"), pad, Text("${tag.interface}", style: TextStyle(fontWeight: FontWeight.bold))]);
      var subtitle = Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [t1,t2,t3,t4]);
      results.add(ListTile(leading: Icon(Icons.barcode_reader), subtitle: SingleChildScrollView(scrollDirection: Axis.horizontal, child: subtitle)));
    }
    children.addAll(results);

    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Zebra123 Plugin Example'),
        ),
        body: SingleChildScrollView(scrollDirection: Axis.vertical, child: Column(children: children, mainAxisSize: MainAxisSize.min)),
      ),
    );
  }

  void callback(ZebraInterfaces interface, ZebraEvents event, dynamic data) {

    switch (event) {

      case ZebraEvents.readBarcode:
        barcodes.clear();
        if (data is List<Barcode>) {
          for (Barcode barcode in data) {
            barcodes.add(barcode);
            if (kDebugMode) print("Barcode: ${barcode.barcode} Format: ${barcode.format} Seen: ${barcode.seen} Interface: ${barcode.interface} ");
          }
        }
        setState((){});
        break;

      case ZebraEvents.readRfid:
        tags.clear();
        if (data is List<RfidTag>) {
          for (RfidTag tag in data) {
            tags.add(tag);
            if (kDebugMode) print("Tag: ${tag.id} Rssi: ${tag.rssi}  Seen: ${tag.seen} Interface: ${tag.interface}");
          }
        }
        setState((){});
        break;

      case ZebraEvents.error:
        if (data is Error) {
          if (kDebugMode) print("Interface: $interface Error: ${data.message}");
        }
        break;

      case ZebraEvents.connectionStatus:
        if (data is ConnectionStatus) {
          if (kDebugMode) print("Interface: $interface ConnectionStatus: ${data.status}");
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

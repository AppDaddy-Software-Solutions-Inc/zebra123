import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zebra123/zebra123.dart';

/// bridge between flutter and android.
class ZebraBridge {

  ZebraInterfaces interface = ZebraInterfaces.unknown;
  ZebraConnectionStatus connectionStatus = ZebraConnectionStatus.disconnected;

  static final _methodChannel = const MethodChannel("dev.fml.zebra123/method");
  static final _eventChannel  = EventChannel('dev.fml.zebra123/event');
  static StreamSubscription<dynamic>? _sink;
  static final List<Zebra123> listeners = [];
  static final List<ZebraInterfaces> support = [];

  // creates a bridge instance
  ZebraBridge({Zebra123? listener}) {
    if (listener != null) {
      addListener(listener);
    }
    _sink = _eventChannel.receiveBroadcastStream().listen(_eventListener);
    _methodChannel.invokeMethod("connect", {"hashCode": hashCode});
  }

  // listen for zebra events
  void addListener(Zebra123 listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  // stop listening to zebra events
  void removeListener(Zebra123 listener) {
    if (listeners.contains(listener)) {
      listeners.remove(listener);
    }
  }

  // invoke scan request
  void scan(ZebraScanRequest request) {
    _methodChannel.invokeMethod("scan", {"request": fromEnumerable(request)});
  }

  // invoke tracking request
  void track(ZebraScanRequest request, {List<String>? tags}) {
    String list = "";
    for (var tag in tags ?? []) {
      if (list == "") {
        list = tag;
      } else {
        list += ",$tag";
      }
    }
    _methodChannel.invokeMethod(
        "track", {"request": fromEnumerable(request), "tags": list});
  }

  // invoke write request
  void write(String epc,
      {String? epcNew, double? password, double? passwordNew, String? data}) {
    _methodChannel.invokeMethod("write", {
      "epc": epc,
      "epcNew": epcNew ?? "",
      "password": (password ?? "").toString(),
      "passwordNew": (passwordNew ?? "").toString(),
      "data": data ?? "",
    });
  }

  // zebra events listener
  void _eventListener(dynamic payload) {
    try {

      final map = Map<String, dynamic>.from(payload);
      interface =
          toEnumerable(map['eventSource'] as String, ZebraInterfaces.values) ??
              interface;
      final event =
          toEnumerable(map['eventName'] as String, ZebraEvents.values) ??
              ZebraEvents.unknown;

      switch (event) {
        case ZebraEvents.readRfid:
          List<RfidTag> list = [];
          List<dynamic> tags = map["tags"];
          for (var i = 0; i < tags.length; i++) {
            var tag = Map<String, dynamic>.from(tags[i]);
            tag["eventSource"] = fromEnumerable(interface);
            list.add(RfidTag.fromMap(tag));
          }

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, list);
          }

          break;

        case ZebraEvents.readBarcode:
          List<Barcode> list = [];
          var tag = Barcode.fromMap(map);
          list.add(tag);

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, list);
          }

          break;

        case ZebraEvents.writeFail:
          var error = Error.fromMap(map);

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, error);
          }
          break;

        case ZebraEvents.writeSuccess:
          var tag = RfidTag.fromMap(map);

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, tag);
          }
          break;

        case ZebraEvents.support:
          if (map.containsKey(fromEnumerable(ZebraInterfaces.rfidapi3))) {
            var supports =
                toBool(map[fromEnumerable(ZebraInterfaces.rfidapi3)]) ?? false;
            if (supports && !support.contains(ZebraInterfaces.rfidapi3)) {
              support.add(ZebraInterfaces.rfidapi3);
            }
          }
          if (map.containsKey(fromEnumerable(ZebraInterfaces.datawedge))) {
            var supports =
                toBool(map[fromEnumerable(ZebraInterfaces.datawedge)]) ?? false;
            if (supports && !support.contains(ZebraInterfaces.datawedge)) {
              support.add(ZebraInterfaces.datawedge);
            }
          }
          break;

        case ZebraEvents.error:
          var error = Error.fromMap(map);

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, error);
          }

          break;

        case ZebraEvents.connectionStatus:
          var connection = ConnectionStatus.fromMap(map);
          connectionStatus = connection.status;

          // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, connection);
          }

          break;

        // unknown event
        default:
        // notify listeners
          for (var listener in listeners) {
            listener.callback(interface, event, null);
          }
          break;
      }
    } catch (e) {
      if (kDebugMode) print(e);
    }
  }
}

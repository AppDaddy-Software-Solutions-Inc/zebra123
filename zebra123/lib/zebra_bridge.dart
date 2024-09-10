import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zebra123/zebra123.dart';

/// bridge between flutter and android.
class ZebraBridge {
  ZebraInterfaces interface = ZebraInterfaces.unknown;
  ZebraConnectionStatus connectionStatus = ZebraConnectionStatus.disconnected;

  static late final StreamSubscription<dynamic> sink;

  static const _methodChannel = MethodChannel("dev.fml.zebra123/method");

  static const _eventChannel = EventChannel('dev.fml.zebra123/event');

  final List<Zebra123> listeners = [];

  static final ZebraBridge _singleton = ZebraBridge._init();

  // creates a bridge instance
  factory ZebraBridge() {
    return _singleton;
  }
  ZebraBridge._init() {
    sink = _eventChannel.receiveBroadcastStream().listen(_eventListener);
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
    _methodChannel.invokeMethod("scan", {"request": fromEnum(request)});
  }

  // zebra events listener
  void _eventListener(dynamic payload) {
    try {
      final map = Map<String, dynamic>.from(payload);
      interface =
          toEnum(map['eventSource'] as String, ZebraInterfaces.values) ??
              interface;
      final event = toEnum(map['eventName'] as String, ZebraEvents.values) ??
          ZebraEvents.unknown;

      switch (event) {
        case ZebraEvents.readRfid:
          List<RfidTag> list = [];
          List<dynamic> tags = map["tags"];
          for (var i = 0; i < tags.length; i++) {
            var tag = Map<String, dynamic>.from(tags[i]);
            tag["eventSource"] = fromEnum(interface);
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

        default:
          break;
      }
    } catch (e) {
      if (kDebugMode) print(e);
    }
  }
}

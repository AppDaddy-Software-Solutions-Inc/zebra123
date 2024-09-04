import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zebra123/zebra123.dart';

class ZebraBridge {

  ZebraInterfaces interface = ZebraInterfaces.unknown;
  ZebraConnectionStatus status = ZebraConnectionStatus.disconnected;

  static late final StreamSubscription<dynamic> sink;

  static const _methodChannel = MethodChannel("dev.fml.zebra123/method");

  static const _eventChannel = EventChannel('dev.fml.zebra123/event');

  List<Zebra123> _listeners = [];

  static final ZebraBridge _singleton = ZebraBridge._init();
  factory ZebraBridge() {
    return _singleton;
  }
  ZebraBridge._init() {
    sink = _eventChannel.receiveBroadcastStream().listen(_eventListener);
    _methodChannel.invokeMethod("connect", {"hashCode": hashCode});
  }

  void addListener(Zebra123 listener) {
    if (!_listeners.contains(listener)) {
      _listeners.add(listener);
    }
  }

  void removeListener(Zebra123 listener) {
    if (_listeners.contains(listener)) {
      _listeners.remove(listener);
    }
  }

  void _eventListener(dynamic payload) {

    try {

      print(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> _eventListener(dynamic payload)");

      final map = Map<String, dynamic>.from(payload);
      interface = toEnum(map['eventSource'] as String, ZebraInterfaces.values) ?? interface;
      final event = toEnum(map['eventName'] as String, ZebraEvents.values) ?? ZebraEvents.unknown;

      switch (event) {

        case ZebraEvents.readRfid:
          List<RfidTag> list = [];
          List<dynamic> tags = map["tags"];
          for (var i = 0; i < tags.length; i++) {
            var tag = Map<String, dynamic>.from(tags[i]);
            list.add(RfidTag.fromMap(tag));
          }

          // notify listeners
          for (var listener in _listeners) {
            listener.callback(interface, event, list);
          }

          break;

        case ZebraEvents.readBarcode:

          List<Barcode> list = [];
          var tag = Barcode.fromMap(map);
          list.add(tag);

          // notify listeners
          for (var listener in _listeners) {
            listener.callback(interface, event, list);
          }

          break;

        case ZebraEvents.error:

          var error = Error.fromMap(map);

          // notify listeners
          for (var listener in _listeners) {
            listener.callback(interface, event, error);
          }

          break;

        case ZebraEvents.connectionStatus:

          var connection = ConnectionStatus.fromMap(map);
          status  = connection.status;

          if (status == ZebraConnectionStatus.disconnected) {
            //_sink = null;
          }

          // notify listeners
          for (var listener in _listeners) {
            listener.callback(interface, event, connection);
          }

          break;

        default:
          break;
      }
    }
    catch (e) {
      if (kDebugMode) print(e);
    }
  }
}
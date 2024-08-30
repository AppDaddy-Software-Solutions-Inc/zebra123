import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:collection/collection.dart';

typedef Callback = void Function(ZebraInterfaces source, ZebraEvents event, dynamic data);

enum Mode {barcode, rfid}

enum ZebraInterfaces {
  zebraSdk,
  dataWedge,
  unknown
}

enum ZebraEvents {
  readRfid,
  readBarcode,
  error,
  connectionStatus,
  unknown
}

enum ZebraConnectionStatus {
  disconnected,
  connected,
  error,
  unknown
}

class Zebra123 {

  static StreamSubscription<dynamic>? sink;

  static const methodChannel = MethodChannel("dev.fml.zebra123/method");

  static const eventChannel = EventChannel('dev.fml.zebra123/event');

  final Callback callback;

  Zebra123({
    required this.callback
  });

  Future connect({String? method}) async {
    sink ??= eventChannel.receiveBroadcastStream().listen(_eventListener);
    methodChannel.invokeMethod("connect", {"method": method});
  }

  Future setMode(String mode) async {
    methodChannel.invokeMethod("mode", {"mode": mode});
  }

  /// Returns a String name given an Enum Type
  static String? fromEnum(Object? e) {
    try {
      return e.toString().split('.').last;
    } catch (e) {
      return null;
    }
  }

  static T? toEnum<T>(String? key, List<T> values) {
    try {
      return values.firstWhereOrNull((v) => key == fromEnum(v));
    } catch (e) {
      return null;
    }
  }

  void _eventListener(dynamic payload) {

    try {

      final   map    = Map<String, dynamic>.from(payload);
      final   source = toEnum(map['eventSource'] as String, ZebraInterfaces.values) ?? ZebraInterfaces.unknown;
      final   event  = toEnum(map['eventName'] as String, ZebraEvents.values) ?? ZebraEvents.unknown;
      dynamic data   = map['data'];

      switch (event) {

        case ZebraEvents.readRfid:
          List<RfidTag> list = [];
          List<dynamic> tags = map["tags"];
          for (var i = 0; i < tags.length; i++) {
            var tag = Map<String, dynamic>.from(tags[i]);
            list.add(RfidTag.fromMap(tag));
          }
          data = list;
          break;

        case ZebraEvents.readBarcode:

          List<Barcode> list = [];
          var tag = Barcode.fromMap(map);
          list.add(tag);
          data = list;
          break;

        case ZebraEvents.error:
          data = Error.fromMap(map);
          break;

        case ZebraEvents.connectionStatus:
          data = ConnectionStatus.fromMap(map);
          break;

        default:
          break;
      }

      // perform callback
      callback(source, event, data);
    }
    catch (e) {
      if (kDebugMode) print(e);
    }
  }
}

class RfidTag {

  String id;
  int antenna;
  int rssi;
  int distance;
  String memoryBankData;
  String lockData;
  int size;
  String seen;

  RfidTag(
  {
    required this.id,
    required this.antenna,
    required this.rssi,
    required this.distance,
    required this.memoryBankData,
    required this.lockData,
    required this.size,
    required this.seen,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'antenna': antenna,
      'rssi': rssi,
      'distance': distance,
      'memoryBankData': memoryBankData,
      'lockData': lockData,
      'size': size,
      'seen': seen,
    };
  }

  factory RfidTag.fromMap(Map<String, dynamic> map) {
    return RfidTag(
      id: map['id'] ?? '',
      antenna: map['antenna']?.toInt() ?? 0,
      rssi: map['rssi']?.toInt() ?? 0,
      distance: map['distance']?.toInt() ?? 0,
      memoryBankData: map['memoryBankData'] ?? '',
      lockData: map['lockData'] ?? '',
      size: map['size']?.toInt() ?? 0,
      seen: map['seen'] ?? '',
    );
  }
}

class Barcode {

  String barcode;
  String format;
  String seen;

  Barcode(
  {
    required this.barcode,
    required this.format,
    required this.seen,
  });

  Map<String, dynamic> toMap() {
    return {
      'barcode': barcode,
      'format': format,
      'seen': seen
    };
  }

  factory Barcode.fromMap(Map<String, dynamic> map) {
    return Barcode(
      barcode: map['barcode'] ?? '',
      format: map['format'] ?? '',
      seen: map['seen'] ?? '',
    );
  }
}

class ConnectionStatus {

  ZebraConnectionStatus status = ZebraConnectionStatus.unknown;

  ConnectionStatus({
    required this.status,
  });

  Map<String, dynamic> toMap() {
    return {
      'status': Zebra123.fromEnum(status) ?? ZebraConnectionStatus.unknown,
    };
  }

  factory ConnectionStatus.fromMap(Map<String, dynamic> map) {
    return ConnectionStatus(
      status: Zebra123.toEnum(map['status'], ZebraConnectionStatus.values) ?? ZebraConnectionStatus.unknown,
    );
  }
}

class Error {

  String source = "";
  String message = "";
  String trace = "";

  Error({
    required this.source,
    required this.message,
    required this.trace,
  });

  Map<String, dynamic> toMap() {
    return {
      'source': source,
      'message': message,
      'trace': trace,
    };
  }

  factory Error.fromMap(Map<String, dynamic> map) {
    return Error(
      source: map['source'] ?? "",
      message: map['message'] ?? "",
      trace: map['trace'] ?? "",
    );
  }
}


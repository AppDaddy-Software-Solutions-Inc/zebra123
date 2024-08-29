import 'dart:async';
import 'package:flutter/services.dart';
import 'dart:convert';

typedef ErrorCallback = void Function(ErrorResult error);
typedef ReadCallback = void Function(List<TagData> data);
typedef ConnectionCallback = void Function(ConnectionStatus status);

enum Mode {barcode, rfid}

class Zebra123 {

  static StreamSubscription<dynamic>? sink;

  static const methodChannel = MethodChannel("dev.fml.zebra123/method");

  static const eventChannel = EventChannel('dev.fml.zebra123/event');

  final ReadCallback readCallback;
  final ConnectionCallback connectionCallback;
  final ErrorCallback errorCallback;

  Zebra123({
    required this.readCallback,
    required this.connectionCallback,
    required this.errorCallback,
  });

  Future connect({String? method}) async {
    sink ??= eventChannel.receiveBroadcastStream().listen(_eventListener);
    methodChannel.invokeMethod("connect", {"method": method});
  }

  Future setMode(String mode) async {
    methodChannel.invokeMethod("mode", {"mode": mode});
  }

  void _eventListener(dynamic event) {

    final map = Map<String, dynamic>.from(event);
    final name = map['eventName'] as String;

    switch (name) {

      case 'ReadRfid':
        List<dynamic> tags = map["tags"];
        List<TagData> list = [];
        for (var i = 0; i < tags.length; i++) {
          list.add(TagData.fromMap(Map<String, dynamic>.from(tags[i])));
        }
        readCallback.call(list);
        break;

     case 'Error':
        var ss = ErrorResult.fromMap(map);
        errorCallback.call(ss);
        break;

      case 'ConnectionStatus':
        ConnectionStatus status = ConnectionStatus.values[map["status"] as int];
        connectionCallback.call(status);
        break;
    }
  }
}

enum ConnectionStatus {
  disconnected,
  connected,
  error
}

class TagData {

  String id;
  int antenna;
  int rssi;
  int distance;
  String memoryBankData;
  String lockData;
  int size;

  TagData({
    required this.id,
    required this.antenna,
    required this.rssi,
    required this.distance,
    required this.memoryBankData,
    required this.lockData,
    required this.size,
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
    };
  }

  factory TagData.fromMap(Map<String, dynamic> map) {
    return TagData(
      id: map['id'] ?? '',
      antenna: map['antenna']?.toInt() ?? 0,
      rssi: map['rssi']?.toInt() ?? 0,
      distance: map['distance']?.toInt() ?? 0,
      memoryBankData: map['memoryBankData'] ?? '',
      lockData: map['lockData'] ?? '',
      size: map['size']?.toInt() ?? 0,
    );
  }

  String toJson() => json.encode(toMap());

  factory TagData.fromJson(String source) =>
      TagData.fromMap(json.decode(source));
}

class ErrorResult {
  int code = -1;
  String errorMessage = "";
  ErrorResult({
    required this.code,
    required this.errorMessage,
  });

  Map<String, dynamic> toMap() {
    return {
      'code': code,
      'errorMessage': errorMessage,
    };
  }

  factory ErrorResult.fromMap(Map<String, dynamic> map) {
    return ErrorResult(
      code: map['code']?.toInt() ?? 0,
      errorMessage: map['errorMessage'] ?? '',
    );
  }

  String toJson() => json.encode(toMap());

  factory ErrorResult.fromJson(String source) =>
      ErrorResult.fromMap(json.decode(source));
}


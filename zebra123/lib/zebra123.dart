import 'dart:async';
import 'package:collection/collection.dart';
import 'package:zebra123/zebra_bridge.dart';

typedef Callback = void Function(ZebraInterfaces interface, ZebraEvents event, dynamic data);

/// Zebra RFID and DataWedge Interface
class Zebra123 {

  late final ZebraBridge _bridge;
  late final Callback _callback;

  ZebraConnectionStatus _connectionStatus = ZebraConnectionStatus.unknown;
  ZebraConnectionStatus get connectionStatus {

    // if listening return zebra bridge connection status
    if (_bridge.listeners.contains(this)) return _bridge.connectionStatus;

    // otherwise return disconnected
    return ZebraConnectionStatus.disconnected;
  }

  Zebra123({required callback}) {
    _callback = callback;
    _bridge = ZebraBridge();
    _bridge.addListener(this);
  }

  Future connect() async {
    if (!_bridge.listeners.contains(this)) {
      _bridge.addListener(this);
      _callback(_bridge.interface, ZebraEvents.connectionStatus, ConnectionStatus(status: connectionStatus));
    }
  }

  Future disconnect() async {
    if (_bridge.listeners.contains(this)) {
      _bridge.removeListener(this);
      _callback(_bridge.interface, ZebraEvents.connectionStatus, ConnectionStatus(status: ZebraConnectionStatus.disconnected));
    }
  }

  void callback(ZebraInterfaces interface, ZebraEvents event, dynamic data) {

    // only report back changes in connection status on change
    if (data is ConnectionStatus) {
      if (data.status != _connectionStatus) {
        _connectionStatus = data.status;
        _callback(interface, event, data);
      }
      return;
    }

    _callback(interface, event, data);
  }

  Future dispose() async {
    disconnect();
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
  ZebraInterfaces interface;

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
    required this.interface
  });

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
      interface: toEnum(map['eventSource'],ZebraInterfaces.values) ?? ZebraInterfaces.unknown,
    );
  }
}

class Barcode {

  String barcode;
  String format;
  String seen;
  ZebraInterfaces interface;

  Barcode(
  {
    required this.barcode,
    required this.format,
    required this.seen,
    required this.interface
  });

  factory Barcode.fromMap(Map<String, dynamic> map) {
    return Barcode(
      barcode: map['barcode'] ?? '',
      format: map['format'] ?? '',
      seen: map['seen'] ?? '',
      interface: toEnum(map['eventSource'],ZebraInterfaces.values) ?? ZebraInterfaces.unknown,
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
      'status': fromEnum(status) ?? ZebraConnectionStatus.unknown,
    };
  }

  factory ConnectionStatus.fromMap(Map<String, dynamic> map) {
    return ConnectionStatus(
      status: toEnum(map['status'], ZebraConnectionStatus.values) ?? ZebraConnectionStatus.unknown,
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

/// Returns a String name given an Enum Type
String? fromEnum(Object? e) {
  try {
    return e.toString().split('.').last;
  } catch (e) {
    return null;
  }
}

/// Returns an Enum Type given a String name
T? toEnum<T>(String? key, List<T> values) {
  try {
    return values.firstWhereOrNull((v) => key == fromEnum(v));
  } catch (e) {
    return null;
  }
}

enum Mode {barcode, rfid}

enum ZebraInterfaces {
  rfidapi3,
  datawedge,
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


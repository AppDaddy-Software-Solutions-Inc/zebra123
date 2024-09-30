import 'dart:async';
import 'package:collection/collection.dart';
import 'package:zebra123/zebra_bridge.dart';

typedef Callback = void Function(
    ZebraInterfaces interface, ZebraEvents event, dynamic data);

/// Zebra RFID and DataWedge Interface
class Zebra123 {
  late final ZebraBridge _bridge;
  late final Callback _callback;

  ZebraConnectionStatus _connectionStatus = ZebraConnectionStatus.unknown;
  ZebraConnectionStatus get connectionStatus {
    // if listening return zebra bridge connection status
    if (ZebraBridge.listeners.contains(this)) return _bridge.connectionStatus;

    // otherwise return disconnected
    return ZebraConnectionStatus.disconnected;
  }

  Zebra123({required callback}) {
    _callback = callback;
    _bridge = ZebraBridge(listener: this);
  }

  // supports the zebra specified interface?
  bool supports(ZebraInterfaces interface) =>
      ZebraBridge.support.contains(interface);

  // listen for zebra events
  Future connect() async {
    if (!ZebraBridge.listeners.contains(this)) {
      _bridge.addListener(this);
      _callback(_bridge.interface, ZebraEvents.connectionStatus,
          ConnectionStatus(status: connectionStatus));
    }
  }

  // stop listening to zebra events
  Future disconnect() async {
    if (ZebraBridge.listeners.contains(this)) {
      _bridge.removeListener(this);
      _callback(_bridge.interface, ZebraEvents.connectionStatus,
          ConnectionStatus(status: ZebraConnectionStatus.disconnected));
    }
  }

  // start scanning for rfid tags
  Future startScanning() async {
    if (ZebraBridge.listeners.contains(this)) {
      _bridge.scan(ZebraScanRequest.rfidStartScanning);
    }
  }

  // stop scanning for rfid tags
  Future stopScanning() async {
    if (ZebraBridge.listeners.contains(this)) {
      _bridge.scan(ZebraScanRequest.rfidStopScanning);
    }
  }

  // start rfid tag tracking
  Future startTracking(List<String> tags) async {
    if (ZebraBridge.listeners.contains(this)) {
      _bridge.track(ZebraScanRequest.rfidStartTracking, tags: tags);
    }
  }

  // stop rfid tag tracking
  Future stopTracking() async {
    if (ZebraBridge.listeners.contains(this)) {
      _bridge.track(ZebraScanRequest.rfidStopTracking);
    }
  }

  // write rfid tag
  Future writeTag(String epc,
      {String? epcNew,
      double? password,
      double? passwordNew,
      String? data}) async {
    _bridge.write(epc,
        epcNew: epcNew,
        password: password,
        passwordNew: passwordNew,
        data: data);
  }

  // zebra event callback handler
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

  // dispose of the zebra plugin
  Future dispose() async {
    disconnect();
  }
}

/// rfid class holds the rfid tag data
class RfidTag {
  String epc;
  int antenna;
  int rssi;
  int distance;
  String memoryBankData;
  String lockData;
  int size;
  String seen;

  // required for write operation
  String? epcNew;
  String? password;
  String? passwordNew;

  ZebraInterfaces interface;

  RfidTag(
      {required this.epc,
      required this.antenna,
      required this.rssi,
      required this.distance,
      required this.memoryBankData,
      required this.lockData,
      required this.size,
      required this.seen,
      required this.interface});

  // create a rfid tag from a map
  factory RfidTag.fromMap(Map<String, dynamic> map) {
    return RfidTag(
      epc: map['epc'] ?? '',
      antenna: map['antenna']?.toInt() ?? 0,
      rssi: map['rssi']?.toInt() ?? 0,
      distance: map['distance']?.toInt() ?? 0,
      memoryBankData: map['memoryBankData'] ?? '',
      lockData: map['lockData'] ?? '',
      size: map['size']?.toInt() ?? 0,
      seen: map['seen'] ?? '',
      interface: toEnumerable(map['eventSource'], ZebraInterfaces.values) ??
          ZebraInterfaces.unknown,
    );
  }
}

/// barcode class holds the rfid tag data
class Barcode {
  String barcode;
  String format;
  String seen;
  ZebraInterfaces interface;

  Barcode(
      {required this.barcode,
      required this.format,
      required this.seen,
      required this.interface});

  /// create a barcode from a map
  factory Barcode.fromMap(Map<String, dynamic> map) {
    return Barcode(
      barcode: map['barcode'] ?? '',
      format: map['format'] ?? '',
      seen: map['seen'] ?? '',
      interface: toEnumerable(map['eventSource'], ZebraInterfaces.values) ??
          ZebraInterfaces.unknown,
    );
  }
}

/// connection status class holds the rfid tag data
class ConnectionStatus {
  ZebraConnectionStatus status = ZebraConnectionStatus.unknown;

  ConnectionStatus({
    required this.status,
  });

  // create a connection status from a map
  factory ConnectionStatus.fromMap(Map<String, dynamic> map) {
    return ConnectionStatus(
      status: toEnumerable(map['status'], ZebraConnectionStatus.values) ??
          ZebraConnectionStatus.unknown,
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
String? fromEnumerable(Object? e) {
  try {
    return e.toString().split('.').last;
  } catch (e) {
    return null;
  }
}

/// Returns an Enum Type given a String name
T? toEnumerable<T>(String? key, List<T> values) {
  try {
    return values.firstWhereOrNull((v) => key == fromEnumerable(v));
  } catch (e) {
    return null;
  }
}

bool? toBool(dynamic s) {
  try {
    if (s == null) return null;
    if (s is bool) return s;
    s = s.toString().trim().toLowerCase();
    if (s == "true") return true;
    if (s == "false") return false;
    return null;
  } catch (e) {
    return null;
  }
}

enum Mode { barcode, rfid }

enum ZebraInterfaces { rfidapi3, datawedge, unknown }

enum ZebraScanRequest {
  rfidStartScanning,
  rfidStopScanning,
  rfidStartTracking,
  rfidStopTracking,
  write,
  unknown
}

enum ZebraEvents {
  readRfid,
  readBarcode,
  error,
  connectionStatus,
  support,
  startRead,
  stopRead,
  writeFail,
  writeSuccess,
  unknown
}

enum ZebraConnectionStatus { disconnected, connected, error, unknown }

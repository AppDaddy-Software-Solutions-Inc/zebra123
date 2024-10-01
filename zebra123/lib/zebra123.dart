import 'dart:async';
import 'package:collection/collection.dart';
import 'package:zebra123/zebra_bridge.dart';

typedef Callback = void Function(
    Interfaces interface, Events event, dynamic data);

/// Zebra RFID and DataWedge Interface
class Zebra123 {
  late final ZebraBridge _bridge;
  late final Callback _callback;

  Status _connectionStatus = Status.unknown;
  Status get connectionStatus {
    // if listening return zebra bridge connection status
    if (_bridge.contains(this)) return _bridge.status;

    // otherwise return disconnected
    return Status.disconnected;
  }

  Zebra123({required callback}) {
    _callback = callback;
    _bridge = ZebraBridge(listener: this);
  }

  // supports the zebra specified interface?
  bool supports(Interfaces interface) => _bridge.supports(interface);

  // listen for zebra events
  Future connect() async {
    if (!_bridge.contains(this)) {
      _bridge.addListener(this);
      _callback(_bridge.interface, Events.connectionStatus,
          ConnectionStatus(status: connectionStatus));
    }
  }

  // stop listening to zebra events
  Future disconnect() async {
    if (_bridge.contains(this)) {
      _bridge.removeListener(this);
      _callback(_bridge.interface, Events.connectionStatus,
          ConnectionStatus(status: Status.disconnected));
    }
  }

  // start scanning for rfid tags
  Future startScanning() async {
    if (_bridge.contains(this)) {
      _bridge.scan(Requests.start);
    }
  }

  // stop scanning for rfid tags
  Future stopScanning() async {
    if (_bridge.contains(this)) {
      _bridge.scan(Requests.stop);
    }
  }

  // start rfid tag tracking
  Future startTracking(List<String> tags) async {
    if (_bridge.contains(this)) {
      _bridge.track(Requests.start, tags: tags);
    }
  }

  // stop rfid tag tracking
  Future stopTracking() async {
    if (_bridge.contains(this)) {
      _bridge.track(Requests.stop);
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
  void callback(Interfaces interface, Events event, dynamic data) {
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

  Interfaces interface;

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
      interface: toEnumerable(map['eventSource'], Interfaces.values) ??
          Interfaces.unknown,
    );
  }
}

/// barcode class holds the rfid tag data
class Barcode {
  String barcode;
  String format;
  String seen;
  Interfaces interface;

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
      interface: toEnumerable(map['eventSource'], Interfaces.values) ??
          Interfaces.unknown,
    );
  }
}

/// connection status class holds the rfid tag data
class ConnectionStatus {
  Status status = Status.unknown;

  ConnectionStatus({
    required this.status,
  });

  // create a connection status from a map
  factory ConnectionStatus.fromMap(Map<String, dynamic> map) {
    return ConnectionStatus(
      status: toEnumerable(map['status'], Status.values) ??
          Status.unknown,
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

enum Interfaces {
  rfidapi3,
  datawedge,
  unknown
}

enum Requests {
  start,
  stop,
  unknown
}

enum Events {
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

enum Status {
  disconnected,
  connected,
  error,
  unknown
}

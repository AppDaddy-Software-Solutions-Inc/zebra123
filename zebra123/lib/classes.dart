import 'enums.dart';
import 'helpers.dart';

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
      interface:
          toEnum(map['eventSource'], Interfaces.values) ?? Interfaces.unknown,
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
      status: toEnum(map['status'], Status.values) ?? Status.unknown,
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

export 'zebra123.dart' show Zebra123;
export 'enums.dart';
export 'classes.dart';

import 'dart:async';
import 'classes.dart';
import 'bridge.dart';
import 'enums.dart';

// callback function for zebra events
typedef Callback = void Function(
    Interfaces interface, Events event, dynamic data);

/// Zebra RFID and DataWedge Interface
class Zebra123 {
  late final Bridge _bridge;
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
    _bridge = Bridge(listener: this);
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

  // return true if bridge contains "this" (listener)
  bool get isListening => _bridge.contains(this);

  // start scanning for rfid tags
  Future startReading() async {
    if (_bridge.contains(this)) {
      _bridge.scan(Requests.start);
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

  // set device mode
  Future setMode(Modes mode) async {
    if (_bridge.contains(this)) {
      _bridge.setMode(mode);
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

/// zebra modes (not implemented)
enum Modes
{
  barcode,
  rfid,
  mixed
}

/// zebra device interfaces
enum Interfaces { rfidapi3, datawedge, unknown }

/// zebra method request parameters
enum Requests { start, stop, unknown }

/// zebra events
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

/// zebra connection status
enum Status { disconnected, connected, error, unknown }

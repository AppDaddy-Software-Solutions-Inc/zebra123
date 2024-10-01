enum Mode { barcode, rfid }

enum Interfaces { rfidapi3, datawedge, unknown }

enum Requests { start, stop, unknown }

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

enum Status { disconnected, connected, error, unknown }

package com.example.connect.config

/**
 * Bluetooth HID configuration and descriptors.
 */
object HidConfig {
    const val HID_APP_NAME = "ConnectControl"
    const val HID_APP_DESCRIPTION = "Connect Remote Control"
    const val HID_MANUFACTURER = "Google"
    
    // Touchscreen (Absolute) Report Descriptor
    // Combo Descriptor (Touchscreen + Mouse)
    val COMBO_REPORT_DESCRIPTOR = byteArrayOf(
        // Touchscreen (Absolute)
        0x05.toByte(), 0x0d.toByte(), // USAGE_PAGE (Digitizer)
        0x09.toByte(), 0x04.toByte(), // USAGE (Touch Screen)
        0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)
        0x09.toByte(), 0x22.toByte(), //   USAGE (Finger)
        0xa1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        0x09.toByte(), 0x42.toByte(), //     USAGE (Tip Switch)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x75.toByte(), 0x07.toByte(), //     REPORT_SIZE (7)
        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     USAGE (X)
        0x09.toByte(), 0x31.toByte(), //     USAGE (Y)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x26.toByte(), 0xff.toByte(), 0x0f.toByte(), // LOGICAL_MAXIMUM (4095)
        0x75.toByte(), 0x10.toByte(), //     REPORT_SIZE (16)
        0x95.toByte(), 0x02.toByte(), //     REPORT_COUNT (2)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        0xc0.toByte(),                 //   END_COLLECTION
        0xc0.toByte(),                 // END_COLLECTION

        // Mouse (Wheel + Right Click)
        0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // USAGE (Mouse)
        0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
        0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
        0x09.toByte(), 0x01.toByte(), //   USAGE (Pointer)
        0xa1.toByte(), 0x00.toByte(), //   COLLECTION (Physical)
        0x05.toByte(), 0x09.toByte(), //     USAGE_PAGE (Button)
        0x19.toByte(), 0x01.toByte(), //     USAGE_MINIMUM (Button 1)
        0x29.toByte(), 0x03.toByte(), //     USAGE_MAXIMUM (Button 3)
        0x15.toByte(), 0x00.toByte(), //     LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(), //     LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(), //     REPORT_SIZE (1)
        0x95.toByte(), 0x03.toByte(), //     REPORT_COUNT (3)
        0x81.toByte(), 0x02.toByte(), //     INPUT (Data,Var,Abs)
        0x95.toByte(), 0x05.toByte(), //     REPORT_COUNT (5)
        0x81.toByte(), 0x03.toByte(), //     INPUT (Cnst,Var,Abs)
        0x05.toByte(), 0x01.toByte(), //     USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x38.toByte(), //     USAGE (Wheel)
        0x15.toByte(), 0x81.toByte(), //     LOGICAL_MINIMUM (-127)
        0x25.toByte(), 0x7f.toByte(), //     LOGICAL_MAXIMUM (127)
        0x75.toByte(), 0x08.toByte(), //     REPORT_SIZE (8)
        0x95.toByte(), 0x01.toByte(), //     REPORT_COUNT (1)
        0x81.toByte(), 0x06.toByte(), //     INPUT (Data,Var,Rel)
        0xc0.toByte(),                 //   END_COLLECTION
        0xc0.toByte()                  // END_COLLECTION
    )
}

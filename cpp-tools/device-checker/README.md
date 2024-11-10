# device-checker

This program is here to check some things in device.

There are two modes:

## Check content

```
./device-checker <device> --check-zero # or -0
./device-checker <device> --check-one  # or -1
```

Check the content of a device:

- `zero`: the whole device must contains only zeros.
- `one`: the whole device must contains only ones.

The usage case is when you use shred to fill device with zeros.

## Find block

```
./device-checker <device> --find=<file> [--sector-size=<size>] # or -f./foobar -b512
```

Chunk the device by blocks of `<size>` bytes, then try to locate one block having exactly the same content than `<file>`.
If `<size>` is not set, then try to guess the device sector size or use 512 as default value.

If the file size is less than block size, then the extraneous byte will be ignored.


Implement Scanner function for FujiXor M215b

Usage:
java -jar FujiXerox_M215b.jar [scan|convert] [options]

Options:
Image Scan [options]
  -h           See this help
  -b           Show progress bar
  -B           background(default 220)
  -c command   Image convert path
  -f           fast scanning
  -r           rgb file with(2496x3507+8)
  -o filename  The output filename
  -p hexid     Product ID in hex (default 0x0165)
  -t           well transparency
  -T           extremely transparency
  -x           term progress
  -v           Vendor ID in hex (default 0x0550)
  -V           verbose mode

Image Convert [options]
  -h           See this help
  -B           background(default 220)
  -o filename  The output filename
  -t           well transparency
  -T           extremely transparency
  -V           verbose mode


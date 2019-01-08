#!/usr/bin/python
# vim: tabstop=2 expandtab shiftwidth=2 fileencoding=utf-8 
# coding=utf-8
# generate {SHA1} {CRC32} {executionTime} {elapsedTime} <{path}>
#

import sys
import os
import errno
import collections
import curses
import time
import hashlib
import zlib
import datetime

def progress(screen, y, current, total):
  (my, mx) = screen.getmaxyx()
  mx = mx - 1

  # consider the progress being done is total is 0 (= empty file, etc)
  if total == 0:
    p=1
  else:
    p=float(current)/total

  before = 'Progress: ['
  after  = '] {:6.2f}%'.format(100*p)

  x = 0
  screen.addstr(y, 0, before, curses.A_NORMAL)
  x = x + len(before)

  length = mx - len(after) - x

  done = int(p * length)

  for i in range(0, length):
    if i < done:
      screen.addch(y, x+i, ord('#'), curses.A_NORMAL)
    else:
      screen.addch(y, x+i, ord(' '), curses.A_NORMAL)
  x = x + length
  screen.addstr(y, x, after, curses.A_NORMAL)

def allProgress(screen, y, current, total, totalBytes, since):
  (my, mx) = screen.getmaxyx()
  mx = mx - 1
  progress(screen, y, current, total)
  centerFormat = '{:^' + str(mx) + '}'
  s = centerFormat.format(
      'Current: {0} | Total: {1} | Bytes read: {2}'.format(current, total, convertBytes(totalBytes))
    )
  screen.addstr(y+1, 0, s, curses.A_NORMAL)
  now = datetime.datetime.now()
  elapsedTime=now-since
  seconds=int(elapsedTime.seconds + 86400*elapsedTime.days)
  if seconds > 0:
    speed=totalBytes / seconds
  else:
    speed=0

  screen.addstr(y+2, 0, centerFormat.format(
    ('Since: {} | Now: {} | Elapsed: {} | Avg. Speed: {}/s'.format(
      since, now, elapsedTime, convertBytes(speed)
    ))[0:mx]
  ))
def convertBytes(bytes):
  if bytes > 1024**4:
    return '{:.3f} TiB'.format(float(bytes)/1024**4)
  if bytes > 1024**3:
    return '{:.3f} GiB'.format(float(bytes)/1024**3)
  if bytes > 1024**2:
    return '{:.3f} MiB'.format(float(bytes)/1024**2)
  if bytes > 1024:
    return '{:.3f} KiB'.format(float(bytes)/1024)
  return str(bytes)

def parse(screen):
  n = len(fullfileset)
  i = 1

  sha1 = hashlib.sha1()
#  crc32 = zlib.
  last = collections.deque(maxlen=100)
  totalBytes=0.0
  executionTime=datetime.datetime.now()
  with open('.integrity-cache-{}'.format(executionTime.strftime('%Y-%m-%d-%H%M%S')), 'w') as cacheHndl:
    cacheHndl.write('# format: {SHA1} {CRC32} {size} {elapsedTime} <{path}>\n')
    cacheHndl.write('execution time: {:12}\n'.format(
      int(round(time.mktime(executionTime.timetuple())*1000))
    ))

    bufferSize=8192
    for path in fullfileset:
      (my, mx) = screen.getmaxyx()
      screen.erase()
      screen.addstr(0, 0, 'File : {}'.format(path))
      screen.addstr(3, 0, '-'*mx)
      screen.addstr(my-4, 0, '-'*mx)
      allProgress(screen, my - 3, i, n, totalBytes, executionTime)

      backlogMax=(my-4-5) / 2
      sy=4
      bl=0
      for data in last:
        screen.addstr(sy  , 0, '  {}'.format(data['path']))
        if data['ok']:
          screen.addstr(sy+1, 0, 
            '    --> Elapsed Time: {0:,}ms Avg. Speed: {3}/s SHA1: {1} CRC32: {2}'.format(
            data['elapsedTime'], data['sha1'], data['crc32'], convertBytes(data['speed'])))
        else:
          screen.addstr(sy+1, 0, '    --> Error: {}'.format(data['error']))

        sy=sy+2
        bl=bl+1
        if bl >= backlogMax:
          break

      try:
        size = os.path.getsize(path)
        screen.addstr(1, 0, 'Size : {:,} bytes ({})'.format(size, convertBytes(size)))

        crc32=0
        startTime = int(round(time.time()*1000))
        with open(path, 'rb') as hndl:
          sha1 = hashlib.sha1()
          j=0
          while True:
            data = hndl.read(bufferSize)
            if data:
              sha1.update(data)
              if not crc32:
                crc32=zlib.crc32(data)
              else:
                crc32=zlib.crc32(data, crc32)
              progress(screen, 2, j, size)
              j = j + bufferSize
              screen.refresh()
            else:
              break
          progress(screen, 2, size, size)
          screen.refresh()
        endTime = int(round(time.time()*1000))
        elapsedTime = endTime - startTime
        seconds=elapsedTime/1000
        if seconds > 0:
          speed = float(size) / seconds
        else:
          speed = 0

        crc32Str='%08X' % (crc32 & 0xffffffff)
        sha1Str=sha1.hexdigest()
        last.appendleft({
          'path': path, 
          'ok' : True, 
          'elapsedTime': elapsedTime, 
          'sha1': sha1Str, 
          'crc32': crc32Str,
          'speed': speed
        })

        cacheHndl.write('{:40} {:8} {:12} {:12} <{}>\n'.format(
          sha1Str, crc32Str, size, elapsedTime, path
        ))
        cacheHndl.flush()

        totalBytes = totalBytes + float(size)
      except IOError as e:
        s = 'IOError: {}: {}'.format(errno.errorcode[e.errno], os.strerror(e.errno))
        screen.addstr(2, 0, s);
        last.appendleft({'path': path, 'ok' : False, 'error': s})

      screen.refresh()
      i = i + 1
  screen.getkey()
  curses.nocbreak()
  screen.keypad(False)
  curses.echo()
#  while True:
#    screen.refresh()

argv    = sys.argv[1:]
fileset = collections.deque()
if len(argv) == 0:
  print('reading from cwd')
  fileset.append(os.getcwd())
else:
  for arg in argv:
    if not os.path.exists(arg):
      print('path <{}> is invalid'.format(arg))
    else:
      print('adding path <{}>'.format(arg))
      fileset.append(arg)

fullfileset = []
while len(fileset) > 0:
  path = fileset.popleft()
  if os.path.isfile(path):
    fullfileset.append(path)
  elif os.path.isdir(path):
    for i in os.listdir(path):
      child = os.path.join(path, i)
      if os.path.isfile(child):
        fullfileset.append(child)
      else:
        fileset.append(child)
  else:
    print("I don't know what kind of file <{}> is".format(path))

print("found {} files".format(len(fullfileset)))
if len(fullfileset) > 0: 
  fullfileset.sort(key = str.lower)
  curses.wrapper(parse)



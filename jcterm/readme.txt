> I am running into some problems with the X11Forwarding example included
in Jsch. 
> Here are the details:
> 
> - Jsch 0.1.44 running on an Ubuntu 11.04 PC and also on a Mac Os X 1.6.8
laptop. 
> - When using Jsch on Ubuntu to do a localhost ssh, I get the infamous
message: 
> "Error: Can't open display: localhost:10.0"
> - I get the same error if I try using Jsch from the Mac to connect to the
Ubuntu 
> PC
> 
> If I use the command line, 'ssh -X localhost' or ('ssh -X hostname' from
the 
> Mac), everything works fine.
> 
> I've seen old posts about this but none of them ever reported a solution
that 
> works.

Yeah, I had the same problem. The problem is that from Java we only can
access the X server by TCP (JSch tries the usual port on 6000+display
number),
while Ubuntu (as some other Linux distributions) lets the X server listen
only on
a Unix domain socket (by default).

I asked this question about this: http://askubuntu.com/q/41330/11138

> Here's what I've tried (unsuccessfully)
> - type 'xhost' in the terminal
> - type 'xhost +127.0.0.1' in the terminal

You should not need to use xhost - the authentication should be better
done by the authentication cookie.

There are essentially these possibilities on Ubuntu:

* configure the X-server to listen on TCP, and then configure
  your firewall not to let anyone from outside in.
  (See http://askubuntu.com/q/34657/11138
for details - you have
   to remove `-nolisten tcp` from the relevant config file.)

* What I finally did, I think: Use `socat` to forward TCP traffic
  to the right Unix port. (The command line is mentioned in my
  answer on http://askubuntu.com/q/41330/11138.)

* Another way could be to use some Java library which provides
  access to Unix domain sockets (usually by JNI/JNA) and somehow
  marry it with JSch (or simply use it for port-forwarding similar
  to socat). Here is a list of some which I bookmarked when I last
  investigated this problem:
     http://stackoverflow.com/q/170600/600500


I'm not sure what is the case on Mac OS - is the X server here
active by default? If so, have a look on its configuration, it
might be the same problem (and similar solutions).
(If you have a `netstat` program, try `netstat -l` and look for
TCP on port 6000 or following, or a unix port named something
with X in it).


Paŭlo
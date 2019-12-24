#!/usr/bin/perl



use strict;
use warnings;


use IO::Socket;
use Time::HiRes qw/time/;
use Getopt::Long qw(GetOptions);

# Author: Jose Quijada
# Create Date: 20170621
# Description: Accepts data piped into it, and forwards it to a server 
#   listening on a port. For now the server and port are hardcoded in 
#   the script.
#
#   Arguments
#     --module - The value here is prepended with "module=", and sent
#       to the server.
#

my $mod_name;
GetOptions('module=s' => \$mod_name) or die "Usage: $0 --module MODULE_NAME\n";

my $server = IO::Socket::INET->new(
    PeerAddr => 'localhost',
    PeerPort => 9000,
    Proto    => 'tcp'
) or die "Can't create client socket: $!";


my $start_time = time;
while (<STDIN>) {
    print $server $_;
}

#if (defined $mod_name && $mod_name !~ /^$/) {
if ($mod_name) {
    print $server 'module=' . $mod_name;
}

shutdown($server, 1);

while (my $data = <$server>) {
    my $tot_time = time - $start_time;
    print "$data";
}

$server->close();

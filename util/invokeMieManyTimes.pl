#!/usr/bin/perl

# Author: Jose Quijada
# Create Date: 20170621
# Description: Asynchronoulsy send ver org data to MIE using child process,
#   via Perl's "fork" function. The input arguments in this order are:
#    - maximum number of ver org's to send
#    - max number of child processes (I.e. threads) to create
#    - path to file that contains input IE data (in XML or JSON format)
#
#   Example:
#   invokeMieManyTimes.pl 100 10 /tmp/test_data.xml 
#
#   This script has dependency on "sendDataToSocket.pl" because it is the
#   script that each child process will execute. It expects output from that 
#   script in the form:
#   <something>||<time_in_seconds>
#
#   This script will then measure seconds it took to process each file
#   invocation and calculate the average.
#   The time measured is taken to be an approximation of how long MIE took 
#   to serve each request, removing as much as possible overhead time doing 
#   anything that is not rule logic processing. This is just an
#   approximation!!! The results will always include some overhead of
#   some kind (E.g. time it took for MIE to receive request, and time it
#   took for response to travel over network if applicable)
#

use strict;
use warnings;

use IO::Handle;
use POSIX ":sys_wait_h";
use Time::HiRes qw/time/;


my $max_invoke_times = $ARGV[0];
my $max_children = $ARGV[1];
my $file_path =  $ARGV[2];


#
# Spawn child processes up to the maximum total number
# of invoke times
#
my $active_children = 0;
my $cnt = 0;
my $start_time = time;
my @cum_times;

#
# The maximum number of threads (child processes) requested 
# can never exceed the max number of IE records to sent to MIE,
# adjust here if that's the case, else we end up submitting
# more than the requested number of IE records further below.
#
if ($max_children > $max_invoke_times) {
    $max_children = $max_invoke_times;
}
while ($cnt < $max_invoke_times) {
    #
    # Spawn children up to max number, and as long as max invoke times
    # has not been exceeded 
    #
    while ($active_children < $max_children) {
        pipe(READER, WRITER);
        WRITER->autoflush(1);
        if (my $pid = fork) {
            close(WRITER);
            my $line;
            chomp($line = <READER>);
            close(READER);
            print STDERR "$line\n";
            my @flds = split('\|\|', $line);
            push @cum_times, $flds[1];
            ++$active_children;
            print STDERR "Spawned child ID $pid, currently $active_children children are active.\n";
            ++$cnt;
        } else {
            die "cannot fork: $!" unless defined $pid;
            close(READER);
            my $send_time = time;
            my $out = `cat $file_path | ./sendDataToSocket.pl`;
            my $tot_time += time - $send_time;
            print WRITER "Child $$ gives output: $out\n";
            close(WRITER);
            exit;
        }
    }


    #
    # If here it means the allowable number of children has been maxed out,
    # harvest all finished child processes. We harvest all as opposed to only
    # the number required to drop below max child level, to make
    # sure all dead children are harvested upon the last iteration of the outer
    # most while loop 
    #
    while (my $kid = waitpid(-1, WNOHANG) > 0) {
        --$active_children;
        print STDERR "Child $kid finished, currently $active_children children are active.\n";
    }
}

my $end_time = time;
my $diff = $end_time - $start_time;
print "Total elapsed time (includes server conn overhead for each"
  . " record!!!)is $diff\n";

#
# Calculate average time it took to process rules for each record. This
# will always include overhead not related to rule logic execution by MIE,
# hence the times will always be higher
#
my $times_sum;
for my $t (@cum_times) {
    $times_sum += $t;
}
my $avg_time = $times_sum/@cum_times;
print "Average time spent in rule logic for each record (includes overhead) "
  . "is $avg_time\n";

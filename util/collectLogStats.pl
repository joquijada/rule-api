#!/usr/bin/perl


use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20170924
# Description: Scan a log file and print out stats on the errors found. The
#   script must be told how to identify the log entries to report on. This is
#   done by specifying a prefix that the script will use to match against
#   each log entry. By default the prefix searched is "ERROR:".
#   You can also specify a regex to use to extract the timestamp from each 
#   log entry of interest. By default the regex used for this is
#   "\[([^\]]+)\]"
#   Example usage:
#     collectLogStats.pl --folder /cygdrive/c/... --search-prefix 'ERROR:'
#  
#   For now the report gets sent to STDOUT, so be sure to capture it to a file
#   if so interested, by redirecting output to the file of choice, I.e.
#   "... > out_file.txt"

use Time::Local;
use Getopt::Long qw(GetOptions);



my $LOG_ENTRY_REGEX_DEF = qr{ERROR:};
my $TS_REGEX_DEF = qr{:\s\[([^\]\{\}]+)\]};
my $DEBUG = "DEBUG";
my %seen_errs;
my %first_seen;
my %last_seen;
my %ocurrences;
my $period_start = 999999999999999999;
my $period_end = 0;
my $folder;
my $ts_regex_arg;
my $search_pattern;
my $debugging;
GetOptions(
  'folder=s' => \$folder,
  'timestamp-regex=s' => \$ts_regex_arg,
  'search-prefix=s' => \$search_pattern,
  'debug' => \$debugging
) or die "Usage: $0 --folder <path> [--search-prefix <prefix> --timestamp-regex <ts_reg_ex>]\n";

if (!$folder) {
    die "Argument --folder is required, please try again and specify "
      . "a valid folder path\n";
}

if (!$ts_regex_arg) {
    $ts_regex_arg = $TS_REGEX_DEF;
}


if (!$search_pattern) {
    $search_pattern = $LOG_ENTRY_REGEX_DEF;
}

if ($debugging) {
    print STDERR "$DEBUG Processing log files located in $folder\n";
}
opendir(IN_FOLDER, "$folder") || die "Could not open folder $folder: $!\n";
if ($debugging) {
    print STDERR "$DEBUG Opened folder $folder\n";
}
my @files = readdir(IN_FOLDER);
close(IN_FOLDER);
for my $file (@files) {
    $file = $folder . $file;
    if ($debugging) {
        print STDERR "$DEBUG Processing file $file\n";
    }
    open(IN_FILE, $file) || die "Could not open file $file: $!\n";
    while(<IN_FILE>) {
        my $cur_line = removeEolChars($_);
        my $err_time = produceTimestampFromLogEntryTime($cur_line, $ts_regex_arg);
        if ($err_time) {
            if ($err_time < $period_start) {
                $period_start = $err_time;
            }

            if ($err_time > $period_end) {
                $period_end = $err_time;
            }
        }
 
        my ($log_entry) = ($cur_line =~ /($search_pattern.+)/);
        if (!$log_entry) {
            next;
        }

        my $log_ent_key = generateLogEntryKey($log_entry);
        ++$seen_errs{$log_ent_key};

        updateTime(\%first_seen, $log_ent_key, $err_time, undef);
        updateTime(\%last_seen, $log_ent_key, $err_time, 1);
    }
    close(IN_FILE);
}



#
# Ready to print out the report
#
$period_start = converTimestampToDateString($period_start);
$period_end = converTimestampToDateString($period_end); 
print "Report start time: $period_start\n";
print "Report end time: $period_end\n";
print "Search pattern: $search_pattern\n\n";
for my $err (sort {$seen_errs{$b} <=> $seen_errs{$a}} keys %seen_errs) {
    my $first = converTimestampToDateString($first_seen{$err});
    my $last = converTimestampToDateString($last_seen{$err});
    print "$err,$first,$last,$seen_errs{$err}\n"
}




#
# Strips the timestamp part and everything else that
# precedes it.
#
sub generateLogEntryKey {
    my $str = $_[0];
    $str =~ s/^.+?\[[^\]]+\]\s+//;
    return $str;
    # ERROR: [Sep-18 17:39:52,460] ie.rule.IeRulesEngineFactory - <p
}

sub removeEolChars {
    my $in = $_[0];
    $in =~ s/[\n\r]+$//;
    return $in;
}



sub updateTime {
    my $hash_ref = $_[0];
    my $key = $_[1];
    my $new_time =  $_[2];
    my $update_if_gt = $_[3]; 
   
    if (!exists $hash_ref->{$key}) {
        $hash_ref->{$key} = $new_time;
        return;
    }

    my $old_time = $hash_ref->{$key};
    if ($debugging) {
        print STDERR "$DEBUG Comparing ts $old_time to $new_time...\n";
    }
    if (($new_time > $old_time && $update_if_gt)
      || ($new_time < $old_time && !$update_if_gt)) {
        $hash_ref->{$key} = $new_time;
        if ($debugging) {
            print STDERR "$DEBUG Update ts $old_time to $new_time\n";
        }
    }
}


sub converTimestampToDateString {
    my @dt_flds = localtime($_[0]);

    my $sec = $dt_flds[0];
    my $min = $dt_flds[1];
    my $hr = $dt_flds[2];
    my $dd = $dt_flds[3];
    my $mm = $dt_flds[4];
    # because month number is 0-based, as per 
    # API http://perldoc.perl.org/Time/Local.html
    $mm = $mm + 1;
    $dd = leftPadNumber($dd);
    $mm = leftPadNumber($mm);
    $min = leftPadNumber($min);
    $hr = leftPadNumber($hr);
    $sec = leftPadNumber($sec);

    my $yr = $dt_flds[5] + 1900;
    return $mm . '/' . $dd . '/' . $yr . ' ' . $hr . ':' . $min . ':' . $sec;
}


sub leftPadNumber {
    my $n = $_[0];
    if ($n < 10) {
        $n = '0' . $n;
    }

    return $n;
}


sub produceTimestampFromLogEntryTime {
    my $log_entry = $_[0];
    my $ts_regex = $_[1];
    if ($debugging) {
        print STDERR "$DEBUG Log entry received is $log_entry, extracting ts...\n";
    }
    my ($time_str) = ($log_entry =~ /$ts_regex/);
    if (!$time_str) {
        return undef;
    }
    if ($debugging) {
        print STDERR "$DEBUG \$time_str is $time_str\n";
    }
    my ($mo, $day, $h, $m, $s) = ($time_str =~ /^(\w{3})-(\d{1,2})\s+(\d{2}):(\d{2}):(\d{2}),\d+$/);   

    my %month_to_code = (
      'Jan' => 0,
      'Feb' => 1,
      'Mar' => 2,
      'Apr' => 3,
      'May' => 4,
      'Jun' => 5,
      'Jul' => 6,
      'Aug' => 7,
      'Sep' => 8,
      'Oct' => 9,
      'Nov' => 10,
      'Dec' => 11
      );

    if ($debugging) {
        print STDERR "$DEBUG Date values extracted are: $mo, $day, $h, $m, $s\n";
    }
    $mo = $month_to_code{$mo};
    my @dt_flds = localtime(time);
    my $yr = $dt_flds[5] + 1900;

    return timelocal($s, $m, $h, $day, $mo, $yr);
}

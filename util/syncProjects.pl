#!/usr/bin/perl


use strict;
use warnings;

use Getopt::Long qw(GetOptions);

my $src_dir;
my $target_dir;
my $debug;
GetOptions(
  'src-dir=s' => \$src_dir,
  'target-dir=s' => \$target_dir,
  'debug' => \$debug
) or die "Usage: $0  [--debug]\n";

my $DEBUG = "DEBUG:";
my $EXT_TO_INCLUDE = [qw(*.java *.js *.properties *.xml *.json *.pl *.sql)];
my $SKIP_ASSET_REGEX = qr{\.idea|target};
my $SKIP_MODIFY_REGEX = qr{\.pl$};


# Slurp file
# Apply replacement RegEx
# Prepare destination path
# Output file to dest path
for my $ext (@$EXT_TO_INCLUDE) {
    if ($debug) {
        print STDERR "Checking $ext\n";
    }
    my $cmd = "find $src_dir -name '$ext'";
    my @found = `$cmd`;
    for my $file (@found) {
        chop($file);
        if ($file =~ /$SKIP_ASSET_REGEX/) {
            if ($debug) {
                print STDERR "$DEBUG Skipping $file because it matched skip pattern "
                  . "$SKIP_ASSET_REGEX\n";
                next;
            }
        }
      
        if ($debug) { 
            print STDERR "$DEBUG Included: $file\n";
        }

        my $output_content = readFileAndApplyReplaceRegEx($file,
          replacementRegEx());

        my $out_file = createOutputPath($file, $src_dir, $target_dir);
        if ($debug) {
            print STDERR "$DEBUG Output file is $out_file\n";
        }
        open(OUT_FILE, "> $out_file") 
          || die "Problem creating file $out_file: $!";
        print OUT_FILE $output_content;
        close(OUT_FILE);
    }
}


sub replacementRegEx {
    my %map = (
      'com.dnb' => 'com.exsoinn',
      'ie-common' => 'rule-api',
      'modern-ie' => 'rule-api-sample',
      'modern-ie-app' => 'rule-api-sample-app'      
      );
 
   
    return \%map; 
}

sub createOutputPath {
    my ($file, $src_dir, $target_dir) = @_;

    my ($path, $file_name) = ($file =~ /^(.+)\/([^\/]+\.\w+)$/);
    if ($debug) {
        print STDERR "$DEBUG \$path is $path, \$file is $file, \$src_dir is $src_dir, \$target_dir is $target_dir\n";
    }
    $path =~ s/$src_dir/$target_dir/;

    # TODO: This is harcoded for now and lack of time
    $path =~ s/com\/dnb/com\/exsoinn/;

    if ($debug) {
        print STDERR "$DEBUG File path to create is $path\n";
    }
    my @cmd = ('mkdir', '--parents', $path); 
    system(@cmd) == 0 || die "Problem creating path $path via command @cmd: $?";
    return $path . '/' . $file_name;
}


sub readFileAndApplyReplaceRegEx {
    my ($file, $reg_ex_map) = @_;
    # slurp in file and store in a variable
    my $file_str;
    {
        local $/ = undef;
        open FILE, "$file" or die "Couldn't open file $file for reading: $!";
        binmode FILE;
        $file_str = <FILE>;
        close FILE;
    }

    if ($file !~ /$SKIP_MODIFY_REGEX/) {
        for my $search_patt (keys %$reg_ex_map) {
            my $replace_patt = $reg_ex_map->{$search_patt};
            $file_str =~ s/$search_patt/$replace_patt/g;
        }
    } else {
        if ($debug) {
            print STDERR "Will not modify $file because it's in the "
              . "skip file modify regex: $SKIP_MODIFY_REGEX\n";
        }
    }

    if ($debug) {
        print STDERR "$DEBUG \$file_str is $file_str\n";
    }
    return $file_str;
}

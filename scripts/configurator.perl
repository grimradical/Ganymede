#!/opt/bin/perl5
#
# This script is designed to walk through the ganymede package
# and make all the build scripts.  It is run by the configure
# script in the root of the ganymede distribution.
#
# $Revision: 1.6 $
# $Date: 1999/01/15 23:25:25 $
#
# Jonathan Abbey
# jonabbey@arlut.utexas.edu
#
############################################################

use Cwd;

#########################################################################
#
#                                                                 resolve
#
# input: $dir - absolute pathname of current directory
#        $link - string containing the readlink() results for a 
#                symbolic link in $dir to be processed
#
# returns: absolute pathname of the target of the symbolic link
#
#########################################################################
sub resolve{
    my($dir, $link) = @_;
    my(@alinkp, $d, $alinkp);

    # make array representations of
    # the current directory and symbolic link

    # if we have a leading / in our $dir or $link,
    # we'll need to shift to get rid of the leading
    # empty array element
  
    @dirp=split(/\//, $dir);
    shift(@dirp) if (!($dirp[0]));
    @linkp=split(/\//, $link);
    shift(@linkp) if (!($linkp[0]));

    # @alinkp is an array that we will build to contain the absolute link
    # target pathname.  If the link does not begin with a /, it is a relative link,
    # and we need to place our current directory into the @alinkp array.  

    if ($link !~ /^\//) {
	@alinkp=@dirp;
    }

    # modify the @alinkp array according
    # to each path component of the @linkp array
    # (an array representation of the symbolic link
    # given to us), to arrive at the ultimate absolute
    # pathname of the symbolic link

    $d = shift(@linkp);

    while ($d) {
	if ($d eq "..") {
	    pop(@alinkp);
	}
	elsif ($d ne "."){		       
	    push(@alinkp, $d);
	}
	$d=shift(@linkp);
    }

    $"='/';

    # perl functions return the value of the last expression
    # in the subroutine

    $alinkp="/@alinkp";
}


#########################################################################
#
#                                                          write_makefile
#
# input: $dir - absolute pathname directory to write makefile into.
#
#########################################################################

sub write_makefile {

    my ($dir) = @_;

    open(MAKEFILE, ">$dir/Makefile") || die("Can't create the $dir/Makefile");

    print MAKEFILE <<ENDMAKEFILE;
#
# Ganymede source makefile
#
# Jonathan Abbey, jonabbey\@arlut.utexas.edu
#

classfiles:
	\@echo "Building Ganymede sources"
	\@cd $rootdir/src/Util; \\
	build		
	\@cd $rootdir/src/JDialog; \\
	build
	\@cd $rootdir/src/JTree; \\
	build
	\@cd $rootdir/src/JTable; \\
	build
	\@cd $rootdir/src/JDataComponent; \\
	build
	\@cd $rootdir/src/server; \\
	build
	\@cd $rootdir/src/JCalendar; \\
	build
	\@cd $rootdir/src/client; \\
	build
	\@cd $rootdir/src/schemas; \\
	build
	\@cd $rootdir/src
	\@echo "Built Ganymede classes"

clean:
	\@echo "Removing class files"
	\@find $rootdir/src/classes -name \*.class -exec rm {} \\; -print

cleanconfig:
	\@echo "Removing config.sh files"
	\@find $rootdir/src -name config.sh -exec rm {} \\; -print
	\@echo
	\@echo "Removed all config files.. re-run $rootdir/configure to regenerate."

jars:
	\@buildJars
ENDMAKEFILE

    close(MAKEFILE);
}

#########################################################################
#
#                                                           write_rebuild
#
# input: $dir - absolute pathname directory to write rebuild script into.
#
#########################################################################

sub write_rebuild {

    my ($dir) = @_;

    open(REBUILDIN, "<$rootdir/scripts/rebuild.in") || die ("Can't read $rootdir/scripts/rebuild.in");
    open(REBUILDOUT, ">$dir/rebuild") || die("Can't create the $dir/rebuild");

    while (<REBUILDIN>){
	s/\/opt\/bin\/perl5/$perlname/;
	print REBUILDOUT $_;
    }

    close(REBUILDOUT);
    close(REBUILDIN);

    chmod 0755, "$dir/rebuild";
}

#########################################################################
#
#                                                            write_config
#
# input: $dir - absolute pathname directory to write config.sh into.
#        $name - name of component being configured
#        $targetdir - where is the class directory for this package?
#        $packagetarget - where are the class files for this package
#        ultimately going to wind up?  Used for rebuild.
#
#########################################################################

sub write_config {

    my($dir, $name, $targetdir, $packagetarget) = @_;

    open(CONFIGFILE, ">$dir/config.sh") || die("Can't create the $dir/config.sh");

    print CONFIGFILE <<ENDCONFIG;
#
# $name config.sh
#
# Auto-generated by $rootdir/configure
#

# Name of component package being built

COMPNAME="$name"

# Location of java compiler

JAVAC=$javadir/javac

# Location of directory containing java executables

JAVADIR=$javadir

# Master location for ganymede classes.. the java compiler
# will look here to find pre-compiled classes needed to compile
# code in this directory

CLASSDIR=$rootdir/src/classes

# Target location for classes built from sources in this directory.
# This is just the root of the tree.. the javac compiler will
# place classes in a subdirectory of this directory according to
# the package name

TARGETDIR=$targetdir

# Ultimate location for class files from the .java files in this
# directory.  Basically TARGETDIR/<package-hierarchy>.  Used by
# rebuild to do partial rebuilds.

PACKAGETARGET=$packagetarget

ENDCONFIG

    close(CONFIGFILE);
}

###
### Let's do it, then.
###

$perlname = $ENV{GPERL};
$rootdir = &resolve(cwd(), $ENV{GROOTDIR});
$javadir = $ENV{GJAVA};

# First we need to put out all the config.sh files that the build and
# rebuild scripts depend on.  See the header for write_config() to
# identify the four pieces.

@configs=("$rootdir/src/Util", "Ganymede Utility Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/Util",
	  "$rootdir/src/JCalendar", "Ganymede Calendar Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/JCalendar",
	  "$rootdir/src/JDialog", "Ganymede Dialog Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/JDialog",
	  "$rootdir/src/JTable", "Ganymede Table Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/JTable",
	  "$rootdir/src/JTree", "Ganymede Tree Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/JTree",
	  "$rootdir/src/JDataComponent", "Ganymede GUI Component Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/JDataComponent",
	  "$rootdir/src/server", "Ganymede Server Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/ganymede",
	  "$rootdir/src/client", "Ganymede Client Classes",
	  "$rootdir/src/classes", "$rootdir/src/classes/arlut/csd/ganymede/client",
	  "$rootdir/src/schemas/bsd", "BSD Schema Classes",
	  "$rootdir/src/schemas/bsd/custom_src/classes",
	  "$rootdir/src/schemas/bsd/custom_src/classes/arlut/csd/ganymede/custom", 
	  "$rootdir/src/schemas/gash", "GASH Schema Classes",
	  "$rootdir/src/schemas/gash/custom_src/classes",
	  "$rootdir/src/schemas/gash/custom_src/classes/arlut/csd/ganymede/custom", 
	  "$rootdir/src/schemas/gasharl", "GASHARL Schema Classes",
	  "$rootdir/src/schemas/gasharl/custom_src/classes",
	  "$rootdir/src/schemas/gasharl/custom_src/classes/arlut/csd/ganymede/custom", 
	  "$rootdir/src/schemas/linux", "Linux Schema Classes",
	  "$rootdir/src/schemas/linux/custom_src/classes",
	  "$rootdir/src/schemas/linux/custom_src/classes/arlut/csd/ganymede/custom", 
	  "$rootdir/src/schemas/nisonly", "NIS Schema Classes",
	  "$rootdir/src/schemas/nisonly/custom_src/classes",
	  "$rootdir/src/schemas/nisonly/custom_src/classes/arlut/csd/ganymede/custom", 
	  "$rootdir/src/schemas/ganymede.old", "Old Ganymede Schema Classes",
	  "$rootdir/src/schemas/ganymede.old/custom_src/classes",
	  "$rootdir/src/schemas/ganymede.old/custom_src/classes/arlut/csd/ganymede/custom");

print "Generating config.sh files in source directories.\n\n";

while ($#configs > 0) {
    write_config(shift @configs, shift @configs, shift @configs, shift @configs);
}

# Now we need to write out the rebuild scripts.  The only reason we're
# doing it here is that rebuild is written in perl, and we want to
# rewrite the header to properly specify the location of perl on this
# system.

@rebuilds=("$rootdir/src/Util",
	   "$rootdir/src/JCalendar",
	   "$rootdir/src/JDialog",
	   "$rootdir/src/JTable",
	   "$rootdir/src/JTree",
	   "$rootdir/src/JDataComponent",
	   "$rootdir/src/server",
	   "$rootdir/src/client",
	   "$rootdir/src/schemas/bsd/custom_src",
	   "$rootdir/src/schemas/gash/custom_src",
	   "$rootdir/src/schemas/gasharl/custom_src",
	   "$rootdir/src/schemas/linux/custom_src",
	   "$rootdir/src/schemas/nisonly/custom_src",
	   "$rootdir/src/schemas/ganymede.old/custom_src");

print "Generating rebuild files in source directories.\n\n";

while ($#rebuilds > 0) {
    write_rebuild(shift @rebuilds);
}


print "Generating $rootdir/src/Makefile\n\n";

write_makefile("$rootdir/src");

print "Done configuring build scripts.\n";

exit;

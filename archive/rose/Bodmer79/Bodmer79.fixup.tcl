#! /usr/bin/tclsh


proc main {argv} {
    set bookid Bodmer79
    set outputdir /mnt/rosecollection/Bodmer79
    set inputdir /mnt/rosecollection/Bodmer79

    foreach line [lrange [split [read stdin] "\n"] 1 end] {
	set line [string trim $line]

	if {$line eq ""} {
	    continue
	}

	set data [split $line ,]

	foreach {maybefolder oldname rotation orig rotation_needed rotation_old newname} $data break
	
	if {$maybefolder ne ""} {
	    set folder $maybefolder
	}

	set oldname [string trim $oldname]
	set newname [string trim $newname]

	if {$oldname eq "*"} {
	    continue
	}

	if {$rotation eq "counter"} {
	    set deg 270
	} elseif {$rotation eq "clock"} {
	    set deg 90
	} else {
	    puts stderr "Bad rotation $rotation for $folder $oldname"
            exit 1
	}

	if {[string length $newname] == 2} {
	    set newname "00$newname"
	} elseif {[string length $newname] == 3} {
	    set newname "0$newname"
	}

	
	regsub film_ $folder "" folder

	if {[string length $oldname] == 1} {
	    set oldname "00$oldname"
	} elseif {[string length $newname] == 2} {
	    set oldname "0$oldname"
	}

	set oldname IMG$oldname.tif

	set input  [file join $inputdir $folder $oldname]

	if {![file exists $input]} {
	    puts stderr "Missing file $input"
	    exit
	}


	set output [file join $outputdir "$bookid.$newname.tif"]

	puts $output
	exec convert -rotate $deg -depth 8 $input $output
    }
}

main $argv
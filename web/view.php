<?php
	#$debug = true;

	# on the assumption that this script is living in web/, then
	# the base directory of everything else will be the base of an 
	# install tarball, ie .. . Most of all, this is so we can set the
	# basedir for the xslt call, so that in turn the xml/procedure.dtd
	# reference in the DOCTYPE declaration will fly.

	chdir("..");
	$basedir = getcwd();
	$baseuri = "file://".$basedir."/";

	$srcdir = $basedir."/doc/examples";
	$outdir = $basedir."/web/cache";

	$now = time();

	$year = date("Y");
	$mon  = date("m");
	$day  = date("D");

	$srcfile = $srcdir."/simpleProcedure_v1_Example.xml";
	$outfile = $outdir."/current.html";
	$xslfile = $basedir."/web/procedureToHTML.xsl";

	# Yes, this is cumbersome, but this level of granularity may help
	# debug the very likely permissions problems that people have.

	if (!file_exists($outdir)) {
		if (!mkdir($outdir)) {
			print "<P>Couldn't make output cache directory $outdir!";
			exit;
		}
	}

	$srcfile_mtime = @filemtime($srcfile);
	$outfile_mtime = @filemtime($outfile);
	$xslfile_mtime = @filemtime($xslfile);

	if ($debug) {
		print "<P><PRE>";

		print "baseuri:  file://".$basedir."/<BR>";
		print "srcfile:  ".$srcfile."<BR>";
		print "xslfile:  ".$xslfile."<BR>";
		print "outfile:  ".$outfile."<BR>";
		print "<BR>";

		print date("D M d G:i:s T Y", $now);
		print ", time now<BR>";
		if ($outfile_mtime == 0) {
			print "----------------------------, cache .html file does not exist<BR>";
		} else {
			print date("D M d G:i:s T Y",$outfile_mtime);
			print ", mtime of cache .html file<BR>";
		}
	
		if ($srcfile_mtime == 0) {
			print "----------------------------, source .xml file does not exist; abort coming up<BR>";
		} else {
			print date("D M d G:i:s T Y",$srcfile_mtime);
			print ", mtime of source .xml file<BR>";
		}

		if ($xslfile_mtime == 0) {
			print "----------------------------, XSL stylesheet (.xsl) missing or not found; abort coming up<BR>";
		} else {
			print date("D M d G:i:s T Y",$xslfile_mtime);
			print ", mtime of .xsl stylesheet file<BR>";
		}
		print "</PRE></P>";
	}

	if ($srcfile_mtime == 0) {
		print "<P>Source .xml file $srcfile does not exist. Aborting</P>";
		exit;
	}
	if ($xslfile_mtime == 0) {
		print "<P>Internally specified XSL stylesheet file $xslfile does not exist. Aborting</P>";
		exit;
	}

	if ($srcfile_mtime > $outfile_mtime) {
		if ($debug) {
			print "<P>[Re]generating file!</P>\n";
		}
		$cache = false;
	} else if ($xslfile_mtime > $outfile_mtime) {
		if ($debug) {
			print "<P>Regenerating file due to XSL stylesheet being newer!</P>\n";
		}
		$cache = false;
	} else {
		if ($debug) {
			print "<P>Using existing file</P>\n";
		}
		$cache = true;
	}

	// special case: changed the XSLT file!
	
?>
<HTML>
<HEAD>
	<TITLE>HTML version of Procedure 
<?
	print "(";

	if ($cache) {
		print "Using cached file";
	} else {
		if ($outfile_mtime == 0) {
			print "Generating cache file";
		} else {
			print "Regenerating cache file";
		}
	}
	print ")";
?>
</TITLE>
</HEAD>
<BODY>
<?

function xslt_trap_error($parser, $num, $level, $messages) {
	global $xslt_trapped_errors;

	if (is_array($messages)) {
		foreach ($messages as $key => $value) {
			$data[$key] = $value;
		}
		$xslt_trapped_errors = sprintf("%s in %s on line %d", $data['msg'], $data['URI'], $data['line']);
	} else {
		$xslt_trapped_errors = $messages;
	}
}

/* 
	The main program....
*/

	global $xslt_trapped_errors;

	if ($cache) {
		include($outfile);
	} else {
		$xh = xsl_create();
		xslt_set_error_handler($xh, xslt_trap_error);
		xslt_set_base($xh, $baseuri);

		if (@xslt_process($xh, $srcfile, $xslfile, $outfile)) {
			include($outfile);
		} else {
			print "<P><B>Oops!</B> $xslt_trapped_errors (XSLT errno: ".xslt_errno($xh).")</P>";
		}
		xslt_free($xh);
	}
?>
</BODY>
</HTML>

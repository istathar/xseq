<?php
	# on the assumption that this script is living in web/, then
	# the base directory of everything else will be the base of an 
	# install tarball, ie .. . Most of all, this is so we can set the
	# basedir for the xslt call, so that in turn the xml/procedure.dtd
	# reference in the DOCTYPE declaration will fly.

	chdir("..");
	$basedir = getcwd();

	$srcdir = $basedir."/doc/examples";

	$now = time();

	$year = date("Y");
	$mon  = date("m");
	$day  = date("D");

	$srcfile = $srcdir."/simpleProcedure_v1_Example.xml";
	$xslfile = $basedir."/web/procedureToHTML.xsl";
?>
<HTML>
<HEAD>
<TITLE>HTML version of Procedure</TITLE>
</HEAD>
<BODY>
<?

	$xslDoc = new DOMDocument();
	$xslDoc->load($xslfile);

	$xmlDoc = new DOMDocument();
	$xmlDoc->load($srcfile);

	$proc = new XSLTProcessor();
	$proc->importStylesheet($xslDoc);

	$result = $proc->transformToXML($xmlDoc);

	if ($result == FALSE) {
		print "<P><B>Oops!</B> $xslt_trapped_errors (XSLT errno: ".libxml_get_last_error().")</P>";
	} else {
		print $result;
	}
?>
</BODY>
</HTML>

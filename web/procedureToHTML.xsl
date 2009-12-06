<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" indent="no" omit-xml-declaration="yes"/>

	<xsl:variable name="num">7</xsl:variable>

	<xsl:template match="/">
		<H1><xsl:value-of select="/procedure/title"/></H1>
		<xsl:if test="/procedure/overview"> 
			<DL>
			<DT><I>Overview</I></DT>
			<DD><xsl:value-of select="/procedure/overview"/></DD>
			</DL>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="/procedure">
		<TABLE noborder="1" width="100%">
		<TR> <TD bgcolor="#006699">Procedure</TD> </TR>
		</TABLE>
		<OL type="I">
		<xsl:apply-templates select="section"/>
		</OL>
	</xsl:template>


	<xsl:template match="section">
		<LI>
			<P><B><xsl:value-of select="@title"/></B></P>
			<P><xsl:value-of select="precis"/></P>
			<OL type="1">
			<xsl:attribute name="start"><xsl:value-of select="count(preceding::step)+1"/></xsl:attribute>
			<xsl:apply-templates select="step"/>
			</OL>
			<HR/>
		</LI>
	</xsl:template>

	<xsl:template match="step">
		<LI>
		<xsl:value-of select="@title"/>
			<UL>
			<xsl:apply-templates select="name"/>
			</UL>
		</LI>
	</xsl:template>

	<xsl:template match="name">
		<LI>
		<xsl:value-of select="@who"/>
			<OL type="a">
			<xsl:apply-templates select="task"/>
			</OL>
		</LI>
	</xsl:template>

	<xsl:template match="task">
		<LI>
		<xsl:value-of select="."/>
		</LI>
	</xsl:template>


</xsl:stylesheet>
<!-- vim: set ts=4: -->

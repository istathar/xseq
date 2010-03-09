<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:p="http://namespace.operationaldynamics.com/procedures/0.4"
	exclude-result-prefixes="p">
	<xsl:output method="html" indent="no" omit-xml-declaration="yes"/>

	<xsl:variable name="num">7</xsl:variable>

	<xsl:template match="/">
		<h1><xsl:value-of select="/p:procedure/p:title"/></h1>
		<xsl:if test="/p:procedure/p:overview"> 
			<dl>
			<dt><i>Overview</i></dt>
			<dd><xsl:value-of select="/p:procedure/p:overview"/></dd>
			</dl>
		</xsl:if>
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="/p:procedure">
		<div style="background-color:#006699; color: white; padding: 5px;">
		Procedure
		</div>
		<ol type="I">
		<xsl:apply-templates select="p:section"/>
		</ol>
	</xsl:template>

	<xsl:template match="p:section">
		<li>
			<p><b><xsl:value-of select="p:title"/></b></p>
			<p><xsl:value-of select="p:precis"/></p>
			<ol type="1">
			<xsl:attribute name="start"><xsl:value-of select="count(preceding::p:step)+1"/></xsl:attribute>
			<xsl:apply-templates select="p:step"/>
			</ol>
			<hr/>
		</li>
	</xsl:template>

	<xsl:template match="p:step">
		<li>
		<xsl:value-of select="p:title"/>
			<ul>
			<xsl:apply-templates select="p:name"/>
			</ul>
		</li>
	</xsl:template>

	<xsl:template match="p:name">
		<li>
		<xsl:value-of select="@who"/>
			<ol type="a">
			<xsl:apply-templates select="p:task"/>
			</ol>
		</li>
	</xsl:template>

	<xsl:template match="p:task">
		<li>
		<xsl:value-of select="."/>
		</li>
	</xsl:template>


</xsl:stylesheet>
<!-- vim: set ts=4: -->

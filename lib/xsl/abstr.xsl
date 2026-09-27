<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="abstr">
  <html>
    <style>
      table, tr, td {
      text-align: center;
      vertical-align: top;
      }
    </style>
    <body>
      <table>
	<tr>
	  <xsl:apply-templates select="astnode"/>
	</tr>
      </table>
    </body>
  </html>
</xsl:template>

<xsl:template match="astnode">
  <td>
    <table width="100%">
      <tr bgcolor="DDDDDD">
	<td colspan="1000">
	  <table width="100%" cellspacing="3pt">
	    <xsl:choose>
	      <xsl:when test="@none">
		<tr>
		  <td>
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    <font style="font-family:helvetica" color="F50A19">
		      <xsl:text>AST</xsl:text>
		    </font>
		    <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  </td>
		</tr>
	      </xsl:when>
	      <xsl:otherwise>
		<tr>
		  <td>
		    <nobr>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>		    
		      <font style="font-size:75%">A<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		      <font style="font-family:arial black">
			<xsl:value-of select="@label"/>
		      </font>
		      <xsl:if test="@name!=''">
			<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
			<font style="font-family:helvetica">
			  <xsl:value-of select="@name"/>
			</font>
		      </xsl:if>
		      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		    </nobr>
		  </td>
		</tr>
		<xsl:if test="location">
		  <tr>
		    <td>
		      <xsl:apply-templates select="location"/>
		    </td>
		  </tr>
		</xsl:if>
	      </xsl:otherwise>
	    </xsl:choose>
	  </table>
	</td>
      </tr>
      <tr>
	<xsl:apply-templates select="astnode"/>
      </tr>
    </table>
  </td>
</xsl:template>

<xsl:template match="location">
  <xsl:choose>
    <xsl:when test="@none">
      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
      <font style="font-family:helvetica" color="F50A19">
	<xsl:text>LOCATION</xsl:text>
      </font>
      <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <nobr>
	<xsl:value-of select="@begline"/>
	<xsl:text>:</xsl:text>
	<xsl:value-of select="@begcolumn"/>
	<xsl:text>-</xsl:text>
	<xsl:value-of select="@endline"/>
	<xsl:text>:</xsl:text>
	<xsl:value-of select="@endcolumn"/>
      </nobr>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>

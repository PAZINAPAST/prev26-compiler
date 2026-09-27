<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="seman">
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
		<xsl:apply-templates select="defat"/>
		<xsl:apply-templates select="istype"/>
		<xsl:apply-templates select="oftype"/>
		<xsl:apply-templates select="isconst"/>
		<xsl:apply-templates select="isaddr"/>
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

<xsl:template match="defat">
  <tr>
    <td bgcolor="FFCF00">
      <nobr>
	<font style="font-family:helvetica; font-size:75%">
	  <xsl:text>defAt: </xsl:text>
	</font>
	<xsl:choose>
	  <xsl:when test="@none">
	    <font style="font-family:helvetica" color="F50A19">
	      <xsl:text>LOCATION</xsl:text>
	    </font>
	  </xsl:when>
	  <xsl:otherwise>
	    <font style="font-size:75%">
	      <xsl:text>A</xsl:text>
	      <xsl:value-of select="@id"/>
	      <xsl:text> @ </xsl:text>
	    </font>
	    <xsl:text> </xsl:text>
	    <xsl:apply-templates select="location"/>
	  </xsl:otherwise>
	</xsl:choose>
      </nobr>
    </td>
  </tr>
</xsl:template>

<xsl:template match="istype">
  <tr>
    <td bgcolor="FFCF00">
      <table width="100%">
	<tr>
	  <td align="center">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>isType:</xsl:text>
	    </font>
	  </td>
	</tr>
	<xsl:choose>
	  <xsl:when test="@none">
	    <tr>
	      <td>
		<font style="font-family:helvetica" color="F50A19">
		  <xsl:text>TYPE</xsl:text>
		</font>
	      </td>
	    </tr>
	  </xsl:when>
	  <xsl:otherwise>
	    <tr>
	      <xsl:apply-templates select="typnode"/>
	    </tr>
	  </xsl:otherwise>
	</xsl:choose>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="oftype">
  <tr>
    <td bgcolor="FFCF00">
      <table width="100%">
	<tr>
	  <td align="center">
	    <font style="font-family:helvetica; font-size:75%">
	      <xsl:text>ofType:</xsl:text>
	    </font>
	  </td>
	</tr>
	<xsl:choose>
	  <xsl:when test="@none">
	    <tr>
	      <td>
		<font style="font-family:helvetica" color="F50A19">
		  <xsl:text>TYPE</xsl:text>
		</font>
	      </td>
	    </tr>
	  </xsl:when>
	  <xsl:otherwise>
	    <tr>
	      <xsl:apply-templates select="typnode"/>
	    </tr>
	  </xsl:otherwise>
	</xsl:choose>
      </table>
    </td>
  </tr>
</xsl:template>

<xsl:template match="typnode">
  <td align="center">
    <div align="center">
      <table cellspacing="1pt">
	<tr bgcolor="FFAF11">
	  <td colspan="1000">
	    <xsl:choose>
	      <xsl:when test="@none">
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		<font style="font-family:helvetica" color="F50A19">
		  <xsl:text>TYPE</xsl:text>
		</font>
		<xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
	      </xsl:when>
	      <xsl:otherwise>
		<nobr>
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		  <xsl:if test="@id!=''">
		    <font style="font-size:75%">T<xsl:value-of select="@id"/>:</font><xsl:text> </xsl:text>
		    <xsl:value-of select="@label"/>
		  </xsl:if>
		  <xsl:text disable-output-escaping="yes"><![CDATA[&nbsp;]]></xsl:text>
		</nobr>
	      </xsl:otherwise>
	    </xsl:choose>
	  </td>
	</tr>
	<xsl:if test="typnode">
	  <tr>
	    <xsl:apply-templates select="typnode"/>
	  </tr>
	</xsl:if>
      </table>
    </div>
  </td>
</xsl:template>

<xsl:template match="isconst">
  <xsl:choose>
    <xsl:when test="@value='none'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica" color="F50A19">
	    <xsl:text>ISCONST</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@value='true'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica; font-size:75%">
	    <xsl:text>isConst</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="isaddr">
  <xsl:choose>
    <xsl:when test="@value='none'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica" color="F50A19">
	    <xsl:text>ISADDR</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
    <xsl:when test="@value='true'">
      <tr>
	<td bgcolor="FFCF00">
	  <font style="font-family:helvetica; font-size:75%">
	    <xsl:text>isAddr</xsl:text>
	  </font>
	</td>
      </tr>
    </xsl:when>
  </xsl:choose>
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

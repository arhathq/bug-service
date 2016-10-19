<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
    <xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes"/>

    <xsl:template match="bug-reports">
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="2cm" margin-bottom="1cm" margin-left="1.5cm" margin-right=".5cm">
                    <fo:region-body region-name="xsl-region-body" margin="0.5cm" margin-top="3cm"/>
                    <fo:region-before region-name="xsl-region-before" margin="0.5cm" extent="3cm" display-align="before" />
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="simpleA4">
                <xsl:apply-templates select="report-header"/>
                <fo:flow flow-name="xsl-region-body">
                    <fo:block/>
                    <xsl:apply-templates select="sla/out-sla-bugs"/>
                </fo:flow>
            </fo:page-sequence>

            <fo:page-sequence master-reference="simpleA4">
                <xsl:apply-templates select="report-header"/>
                <fo:flow flow-name="xsl-region-body">
                    <xsl:apply-templates select="sla/p1-sla"/>
                    <xsl:apply-templates select="sla/p2-sla"/>
                    <fo:block id='end'/>
                </fo:flow>
            </fo:page-sequence>

        </fo:root>
    </xsl:template>

    <xsl:template match="report-header">
        <fo:static-content flow-name="xsl-region-before">
            <fo:block font-size="12pt" font-weight="bold" text-align="center" space-after="5mm" color="white" background-color="#365f91" border-color="black" border-width="1pt" border-style="solid">
                <fo:block>SLA Report, Week</fo:block>
                <fo:block>#<xsl:value-of select="format-dateTime(date, '[W]')"/></fo:block>
                <fo:block><xsl:value-of select="format-dateTime(date, '[D1o] [MNn], [Y]', 'en', (), ())"/></fo:block>
                <fo:block text-align="outside">
                    Page <fo:page-number font-style="normal" /> of <fo:page-number-citation ref-id='end'/>
                </fo:block>
            </fo:block>
        </fo:static-content>
    </xsl:template>

    <xsl:template match="out-sla-bugs">
        <fo:block font-size="12pt" font-weight="bold" space-after="5mm">P1/P2 out of SLA</fo:block>
        <fo:block font-size="10pt">
            P1: <xsl:value-of select="p1-bugs"/>  P2: <xsl:value-of select="p2-bugs"/>
        </fo:block>
        <xsl:apply-templates select="list"/>
        <!--<fo:block text-align="center">-->
            <!--<fo:external-graphic src="url('data:{image/content-type};base64,{image/content-value}')" content-height="50%" scaling="uniform"/>-->
        <!--</fo:block>-->
    </xsl:template>

    <xsl:template match="list[bug]">
        <fo:block font-size="10pt" space-after=".5cm" space-before=".5cm" margin-left="0">
            <fo:table width="100%" border-collapse="collapse">
                <fo:table-header font-weight="bold" text-align="justify">
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="1.5cm">
                        <fo:block>Bug ID</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="1.5cm">
                        <fo:block>Priority</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
                        <fo:block>Opened</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
                        <fo:block>Resolved</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="1.5cm">
                        <fo:block>Days open</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="2cm">
                        <fo:block>Reopened</fo:block>
                    </fo:table-cell>
                    <fo:table-cell border="solid .5px black" padding="1em" padding-left=".5em" padding-right=".5em" width="7cm">
                        <fo:block>Summary</fo:block>
                    </fo:table-cell>
                </fo:table-header>
                <fo:table-body>
                    <xsl:apply-templates select="bug"/>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <xsl:template match="bug">
        <fo:table-row>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="id"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="priority"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="format-dateTime(opened, '[M01]/[D01]/[Y0001] [H01]:[m01]', 'en', (), ())"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:choose>
                        <xsl:when test="resolved != ''"><xsl:value-of select="format-dateTime(resolved, '[M01]/[D01]/[Y0001] [H01]:[m01]', 'en', (), ())"/></xsl:when>
                        <xsl:otherwise/>
                    </xsl:choose>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="daysOpen"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="reopenedCount"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell border="solid .5px black" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="summary"/>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </xsl:template>

    <xsl:template match="p1-sla">
        <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">P1 SLA Achievement:</fo:block>
        <fo:block font-size="10pt">
            <fo:table width="100%" border-collapse="collapse">
                <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
                    <fo:table-cell width="5cm" padding-left=".5em" padding-top=".5em">
                        <fo:block text-align="left">Week</fo:block>
                    </fo:table-cell>
                    <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
                        <fo:block>Out SLA</fo:block>
                    </fo:table-cell>
                    <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
                        <fo:block>Total</fo:block>
                    </fo:table-cell>
                </fo:table-header>
                <fo:table-body text-align="right">
                    <xsl:apply-templates select="sla-achievement/week-period"/>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <xsl:template match="p2-sla">
        <fo:block font-size="12pt" font-weight="bold" space-before="1.5em" space-after=".5em">P2 SLA Achievement:</fo:block>
        <fo:block font-size="10pt">
            <fo:table width="100%" border-collapse="collapse">
                <fo:table-header background-color="#ddebf7" font-weight="bold" border-bottom="solid .5px #9cc2e5" text-align="right">
                    <fo:table-cell width="5cm" padding-left=".5em" padding-top=".5em">
                        <fo:block text-align="left">Week</fo:block>
                    </fo:table-cell>
                    <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
                        <fo:block>Out SLA</fo:block>
                    </fo:table-cell>
                    <fo:table-cell width="3cm" padding-right=".5em" padding-top=".5em">
                        <fo:block>Total</fo:block>
                    </fo:table-cell>
                </fo:table-header>
                <fo:table-body text-align="right">
                    <xsl:apply-templates select="sla-achievement/week-period"/>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <xsl:template match="sla-achievement/week-period">
        <fo:table-row>
            <xsl:if test="week = 'Grand Total'">
                <xsl:attribute name="font-weight">bold</xsl:attribute>
                <xsl:attribute name="background-color">#ddebf7</xsl:attribute>
                <xsl:attribute name="border-top">solid .5px #9cc2e5</xsl:attribute>
            </xsl:if>
            <fo:table-cell text-align="left" padding-left=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="week"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding-right=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="count"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding-right=".5em" padding-top=".5em">
                <fo:block>
                    <xsl:value-of select="totalCount"/>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </xsl:template>

</xsl:stylesheet>
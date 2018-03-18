# ComputerCraftFontMaker
A font maker for ComputerCraft

Usage:
java makefont.MakeFont <path-to-ttf> <ttf-font-name> <type> <character-size> <output-path> <fontname>

The font should be a legal monspaced font. The characters will be printed by using the width of zero ('0').
type will be one of "0" (plain), "1" (bold) or "2" (italic). See java.awt.Font class for details.

# Example for using gnu unifont:
	
First download the regular ttf at http://unifoundry.com/unifont.html (12 MBytes at the moment).
Save it where you like to have it:
Start makefont with following arguments:
	unifont-10.0.07.ttf
	Unifont
	0
	20
	.
	Unifont
	
This will produce two files: Unifont.png and Unifont.properties.
Put them inside a resource pack at path assets/computercraft/textures/gui/fonts and restart your minecraft.

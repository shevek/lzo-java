TARGETS=

all : ${TARGETS}

.PRECIOUS : %.png

%.png : %.dot
	dot -Tpng -o $*.png $*.dot

%.map : %.dot
	dot -Tcmapx -o $*.map $*.dot

%.eog : %.png
	eog $*.png

clean:
	rm -f ${TARGETS}

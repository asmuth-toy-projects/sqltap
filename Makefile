# This file is part of the "sqltap" project, http://github.com/dawanda/sqltap>
#   (c) 2016 Christian Parpart <christian@dawanda.com>
#
# Licensed under the MIT License (the "License"); you may not use this
# file except in compliance with the License. You may obtain a copy of
# the License at: http://opensource.org/licenses/MIT

IMAGE = sqltap
VERSION = $(shell grep ^version build.sbt | cut -d\" -f2)

image:
	docker build -t ${IMAGE}:${VERSION} .

push: image
	docker push ${IMAGE}:${VERSION}

clean:
	rm -rf target

.PHONY: image clean push

# vim:ts=8:noet

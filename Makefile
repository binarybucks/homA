

npm_update: 
		 				find -d . -name package.json -depth 3 -print0 | xargs -0 -IX sh -c 'cd `dirname X` && echo Updating node modules in $(pwd) && npm update'


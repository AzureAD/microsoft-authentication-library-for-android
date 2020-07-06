
In order to update the common submodule, we need to pull the latest version.

In this flow, you may want to use a branch that isn't dev, since we may
be using code that is experimental, or represents a fix or a patch.  In
that event, you should checkout whatever branch is appropriate.

```
 cd common
 git checkout dev
 git pull
 cd ..
 git add common
 git commit
```

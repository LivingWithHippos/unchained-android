# Contribution guidelines

Development happens on the `dev` branch, which is merged onto `master` when ready to be published on the stores. To contribute to the project, open an [issue](https://github.com/LivingWithHippos/unchained-android/issues) or a [discussion](https://github.com/LivingWithHippos/unchained-android/discussions) to talk about it with the maintainers.

## How Can I Contribute?

### Reporting Bugs

This section guides you through submitting a bug report for Unchained. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior, and find related reports.

Before creating bug reports, please check [this list,](https://github.com/LivingWithHippos/unchained-android/issues?q=is%3Aissue+is%3Aopen+label%3Abug) as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible. Fill out [the required template](https://github.com/LivingWithHippos/unchained-android/blob/master/.github/ISSUE_TEMPLATE/bug_report.md), the information it asks for helps us resolve issues faster.

> **Note:** If you find a **Closed** issue that seems like it is the same thing that you're experiencing, open a new issue and include a link to the original issue in the body of your new one.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for Unchained, including completely new features and minor improvements to existing functionality. Following these guidelines helps maintainers and the community understand your suggestion and find related suggestions.

Before creating enhancement suggestions, please check [this list,](https://github.com/LivingWithHippos/unchained-android/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement) as you might find out that you don't need to create one. When you are creating an enhancement suggestion, please include as many details as possible. Fill in [the template](https://github.com/LivingWithHippos/unchained-android/blob/master/.github/ISSUE_TEMPLATE/feature_request.md), including the steps that you imagine you would take if the feature you're requesting existed.


### Your First Code Contribution

Unsure where to begin contributing? You can start by looking through the `good first issue` and `help-wanted` issues. If you don't know how to code in Kotlin for Android, you can still fix or write a new translation, or make the README.md or any other .md file better.

#### Local development

Unchained is developed on [Android Studio](https://developer.android.com/studio), installed and kept up-to-date with [Jetbrains Toolbox](https://www.jetbrains.com/toolbox-app/).

It should work "out of the box".

#### Repository set up

Some basic git options are explained [here.](https://guides.github.com/introduction/git-handbook/) My suggestion is to use [Github Desktop](https://desktop.github.com/) ([Linux fork](https://github.com/shiftkey/desktop)).

Fork this repository, change the branch to dev and create a new branch from that with a meaningful name like "fix-dns-https". After having written your code, you can open a PR to get it merged with this repository.

[This](https://github.com/firstcontributions/first-contributions) is a pretty clear guide with some pics but there are many other, like [this](https://www.dataschool.io/how-to-contribute-on-github/).

The important part to keep in mind is that after forking and cloning it on your pc you need to switch the `dev` branch before creating a new branch:

- `git branch -a` will show you the branches
- `git checkout dev` will switch to `dev`

#### Linting

Before opening a PR check the code style with `gradlew ktLintCheck`. You may get some fixes suggested, but follow them only if they are about the code you wrote.

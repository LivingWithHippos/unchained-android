# Search Plugins

Search plugins can be added from the search screen by clicking on the manage repository button. They let the user search for files on a specific website. The extension is `.unchained` but it is just json.

## Install plugins

Plugins can be installed in multiple ways:

- download a plugin and click on it, select Unchained when asked which app to use
- share a link to a plugin file to Unchained
- add a repository link from the plugins screen and then refresh the plugins list

## Uninstall plugins

- a button to remove all the plugins is available in the settings menu
- using the plugins screen, click on the remove button

## Create a new repository
The repository json structure is pretty simple:

```json5
{
    // this is the repository engine version used for future development, not the version of the json file
    "repository_version": 1.0,
    "name": "Unchained",
    "description": "Unchained main repository",
    "author": "LivingWithHippos",
    // plugins list
    "plugins": [
    {
        // the id MUST match the one inside the plugin file
        "id": "etree",
        // every plugin can have different versions. The version should match the one in the plugin file
        "versions": [
        {
            // version of the plugin itself, chosen by the author
            "plugin": 2.0,
            // engine version of the plugin, used to check for compatibility with Unchained
            "engine": 2.0,
            // link to the plugin file itself
            "link": "https://gitlab.com/LivingWithHippos/unchained-plugins/-/raw/main/repository/plugins/etree/etree_v2.0.unchained"
        }
        ]
    }
    ]
}
```

If you don't know where to publish your repository and plugins, just create a project on Github, Gitlab, Sourcehat, Codeberg or whatever and use that.

**N.B. the link to the repository to be used/shared must directly open the text file, test it in your browser. On Github you can get it by pressing the "raw" button on the file page.**

## Create a new Plugin

The structure of a plugin file is more complex to allow for the necessary flexibility for every website layout. Websites requiring login or protected by Cloudflare are not searchable yet.

To create a new plugin you can take inspiration from the "official" ones. The basic skills required are:

- some knowledge of json
- some knowledge of html
- some knowledge of regular expressions

Check the `template.json5` file under `extra_assets/plugins` for a more detailed explanation of the various fields.

Websites are classified by how we can retrieve the download links from the search page. Magnet/Torrents and hosting files are supported.

**Internal links**

The search page provides a list of links that points to the searched item page, which contains the download links. It's the slowest since a web request must be made for every result.

Search page -> Details page -> download links

**Direct links**

The search page has the links available directly on it. Every result is separated by an html class common for every search result.

Search page -> html table class/id -> download links

**Direct links on table**

The search page has the links available directly on it. Every result is a row in an html table.

Search page -> html table class -> download links

**Indirect links on table**

The search page has the results in an html table but the download links are in the details page. Similar to the "internal links" one.

Search page -> html table class -> Details page -> download links

___

Every search to be performed on a page for links or details (the result name, seeders etc.) has the same form:

```json5
// the field we are searching for. These are fixed, seeders, leechers, size, name...
"field name": {
    // which regex to use if multiple ones are available
    "regex_use": "first",
    // list of regexes
    "regexps": [
    {
        // the regex itself. Java style.
        "regex": "href=\"(/torrent/[^\"]+)",
        // group capturing is supported. Always add one.
        "group": 1,
        // how to treat the result. Is it completed, partial...
        "slug_type": "append_url",
        // optional field to be prefixed to the link found if slug type is append_url
        "other": "https://other_url.com"
    }
    ]
}
```

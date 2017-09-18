const {SpellCheckHandler, ContextMenuListener, ContextMenuBuilder} = require('electron-spellchecker');
const log = require('electron-log');

window.spellCheckHandler = new SpellCheckHandler();
window.spellCheckHandler.attachToInput();

log.info('Attaching spellcheck handler');

window.spellCheckHandler.switchLanguage('en-US');
window.spellCheckHandler.autoUnloadDictionariesOnBlur();

window.contextMenuBuilder = new ContextMenuBuilder(window.spellCheckHandler);
window.contextMenuListener = new ContextMenuListener((info) => {
    window.contextMenuBuilder.showPopupMenu(info);
});

window.OBSERVER = true;
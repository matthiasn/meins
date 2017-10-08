window.OBSERVER = true;

const {ipcRenderer} = require('electron');

window.send = (s) => {
    console.log(s);
    ipcRenderer.send('relay', s);
};

// const {SpellCheckHandler, ContextMenuListener, ContextMenuBuilder} = require('electron-spellchecker');
// const log = require('electron-log');
//
// window.spellCheckHandler = new SpellCheckHandler();
// window.spellCheckHandler.attachToInput();
//
// log.info('Attaching spellcheck handler');
//
// window.spellCheckHandler.switchLanguage('en-US');
// window.spellCheckHandler.autoUnloadDictionariesOnBlur();
//
// window.contextMenuBuilder = new ContextMenuBuilder(window.spellCheckHandler);
// window.contextMenuListener = new ContextMenuListener((info) => {
//     window.contextMenuBuilder.showPopupMenu(info);
// });

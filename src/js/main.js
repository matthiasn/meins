window.deps = {
    'react' : require('react'),
    'react-dom' : require('react-dom'),
    'react-addons-perf' : require('react-addons-perf'),
    'draft-js' : require('draft-js'),
    'react-responsive-carousel' : require('react-responsive-carousel').Carousel,
    'editor' : require ('./editor'),
    'SearchFieldEditor' : require ('./SearchFieldEditor'),
    'EntryTextEditor' : require ('./EntryTextEditor'),
    'Calendar' : require ('./calendar'),
    'BigCalendar' : require ('./bigCalendar'),
    'emojiFlags' : require('emoji-flags')
};

window.React = window.deps['react'];
window.ReactDOM = window.deps['react-dom'];
window.Draft = window.deps['draft-js'];
window.ReactPerf = window.deps['react-addons-perf'];
window.MyEditor = window.deps['editor'];

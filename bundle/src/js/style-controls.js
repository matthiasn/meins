import React, {Component} from 'react';
import Editor, {createEditorStateWithText} from 'draft-js-plugins-editor'; // eslint-disable-line import/no-unresolved
import createMentionPlugin, {defaultSuggestionsFilter} from 'draft-js-mention-plugin'; // eslint-disable-line import/no-unresolved
import {fromJS} from 'immutable';
import editorStyles from './editorStyles.css';
import {RichUtils} from 'draft-js';

class StyleButton extends Component {
    constructor() {
        super();
        this.onToggle = (e) => {
            e.preventDefault();
            this.props.onToggle(this.props.style);
        };
    }

    render() {
        let className = 'fa ' + this.props.icon;
        if (this.props.active) {
            className += ' RichEditor-activeButton';
        }
        return (
            <span className={className}
                  onMouseDown={this.onToggle}>{this.props.label}</span>
        );
    }
}

const INLINE_STYLES = [
    {label: 'Bold', style: 'BOLD', icon: 'fa-bold fa-wide'},
    {label: 'Italic', style: 'ITALIC', icon: 'fa-italic fa-wide'},
    {label: 'Underline', style: 'UNDERLINE', icon: 'fa-underline fa-wide'},
    {label: 'Monospace', style: 'CODE', icon: 'fa-code fa-wide'},
    {label: 'strike', style: 'STRIKETHROUGH', icon: 'fa-strikethrough fa-wide'},
];

const BLOCK_TYPES = [
    {style: 'header-one', icon: 'fa-header'},
    {style: 'header-two', icon: 'fa-header header-2'},
    {style: 'header-three', icon: 'fa-header header-3'},
    {style: 'unordered-list-item', icon: 'fa-list-ul fa-wide'},
    {style: 'ordered-list-item', icon: 'fa-list-ol fa-wide'},
    {style: 'code-block', icon: 'fa-code'},
];

const StyleControls = (props) => {
    const {editorState, state, show} = props;
    const selection = editorState.getSelection();
    const blockType = editorState
        .getCurrentContent()
        .getBlockForKey(selection.getStartKey())
        .getType();
    const currentStyle = editorState.getCurrentInlineStyle();

    if (props.show) {
        return (
            <div className="RichEditor-controls edit-menu">
                {INLINE_STYLES.map(type =>
                    <StyleButton
                        key={type.style}
                        active={currentStyle.has(type.style)}
                        icon={type.icon}
                        onToggle={props.onToggleInline}
                        style={type.style}
                    />
                )}
                {BLOCK_TYPES.map((type) =>
                    <StyleButton
                        key={type.style}
                        active={type.style === blockType}
                        icon={type.icon}
                        onToggle={props.onToggleBlockType}
                        style={type.style}
                    />
                )}
            </div>
        );
    }
    return null;
};

export default StyleControls

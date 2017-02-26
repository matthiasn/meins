import React, {Component} from 'react';
import Editor, {createEditorStateWithText} from 'draft-js-plugins-editor'; // eslint-disable-line import/no-unresolved
import createMentionPlugin, {defaultSuggestionsFilter} from 'draft-js-mention-plugin'; // eslint-disable-line import/no-unresolved
import {fromJS} from 'immutable';
import editorStyles from './editorStyles.css';

export default class SearchFieldEditor extends Component {

    state = {};

    onSearchChange = ({value}) => {
        this.setState({
            mentionSuggestions: defaultSuggestionsFilter(value, this.state.mentions),
        });
    };

    onSearchChange2 = ({value}) => {
        this.setState({
            hashtagSuggestions: defaultSuggestionsFilter(value, this.state.hashtags),
        });
    };

    focus = () => {
        this.editor.focus();
    };

    onAddMention = () => {
        // get the mention object selected
    };

    constructor(props) {
        super(props);
        console.log(props);

        this.state.editorState = props.editorState;

        const hashtagPlugin = createMentionPlugin({
            mentionTrigger: "#",
        });

        const mentionPlugin = createMentionPlugin({
            //mentionPrefix: "@",
            mentionTrigger: "@",
        });

        this.plugins = [hashtagPlugin, mentionPlugin];
        this.HashtagSuggestions = hashtagPlugin.MentionSuggestions;
        this.MentionSuggestions = mentionPlugin.MentionSuggestions;

        this.state.mentions = fromJS(props.mentions);
        this.state.hashtags = fromJS(props.hashtags);

        this.state.mentionSuggestions = fromJS(props.mentions);
        this.state.hashtagSuggestions = fromJS(props.hashtags);

        this.onChange = (editorState) => {
            props.onChange(editorState);
            this.setState({editorState});
        };
    }

    render() {
        const HashtagSuggestions = this.HashtagSuggestions;
        const MentionSuggestions = this.MentionSuggestions;
        return (
            <div className="search-field"
                 onClick={this.focus}>
                <Editor
                    editorState={this.state.editorState}
                    //editorState={this.props.editorState}
                    onChange={this.onChange}
                    plugins={this.plugins}
                    ref={(element) => {
                        this.editor = element;
                    }}
                />
                <MentionSuggestions
                    onSearchChange={this.onSearchChange}
                    suggestions={this.state.mentionSuggestions}
                    onAddMention={this.onAddMention}
                />
                <HashtagSuggestions
                    onSearchChange={this.onSearchChange2}
                    suggestions={this.state.hashtagSuggestions}
                    onAddMention={this.onAddMention}
                />
            </div>
        );
    }
}

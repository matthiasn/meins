import React, {Component} from 'react';
import Editor, {createEditorStateWithText} from 'draft-js-plugins-editor'; // eslint-disable-line import/no-unresolved
import createMentionPlugin, {defaultSuggestionsFilter} from 'draft-js-mention-plugin'; // eslint-disable-line import/no-unresolved
import {fromJS} from 'immutable';
import editorStyles from './editorStyles.css';

const suggestionsFilter = (searchValue, suggestions) => {
    const value = searchValue.toLowerCase();
    const filteredSuggestions = suggestions.filter((suggestion) => {
        const name = suggestion.get("name").toLowerCase();
        const match = name.indexOf(value);
        return match > -1;
    });
    const size = filteredSuggestions.size < 15 ? filteredSuggestions.size : 15;
    return filteredSuggestions.setSize(size);
};

export default class SearchFieldEditor extends Component {

    state = {};

    onSearchChange = ({value}) => {
        this.setState({
            storySuggestions: suggestionsFilter(value, this.state.stories),
        });
    };

    focus = () => {
        this.editor.focus();
    };

    onAddStory = (story) => {
        this.selectStory(story.get("id"));
    };

    constructor(props) {
        super(props);
        this.state.editorState = props.editorState;

        const storyPlugin = createMentionPlugin({
            mentionTrigger: "$",
        });

        this.selectStory = props.selectStory;
        this.plugins = [storyPlugin];
        this.StorySuggestions = storyPlugin.MentionSuggestions;
        this.state.stories = fromJS(props.stories);
        this.state.storySuggestions = fromJS(props.stories);

        this.onChange = (editorState) => {
            props.onChange(editorState);
            this.setState({editorState});
        };
    }

    render() {
        const StorySuggestions = this.StorySuggestions;
        return (
            <div className="search-field"
                 onClick={this.focus}>
                <Editor
                    editorState={this.state.editorState}
                    onChange={this.onChange}
                    plugins={this.plugins}
                    ref={(element) => {
                        this.editor = element;
                    }}
                />
                <StorySuggestions
                    onSearchChange={this.onSearchChange}
                    suggestions={this.state.storySuggestions}
                    onAddMention={this.onAddStory}
                />
            </div>
        );
    }
}

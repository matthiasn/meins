import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_form_builder/flutter_form_builder.dart';
import 'package:wisely/blocs/sync/classes.dart';
import 'package:wisely/blocs/sync/encryption_cubit.dart';
import 'package:wisely/theme.dart';
import 'package:wisely/widgets/sync/qr_widget.dart';

class EmailConfigForm extends StatefulWidget {
  const EmailConfigForm({Key? key}) : super(key: key);

  @override
  _EmailConfigFormState createState() {
    return _EmailConfigFormState();
  }
}

class _EmailConfigFormState extends State<EmailConfigForm> {
  final _formKey = GlobalKey<FormBuilderState>();

  @override
  Widget build(BuildContext context) {
    if (Platform.isIOS || Platform.isAndroid) {
      return BlocBuilder<EncryptionCubit, EncryptionState>(
          builder: (context, EncryptionState state) {
        return Center(
          child: state.maybeWhen(
              (sharedKey, imapConfig) =>
                  StatusTextWidget(imapConfig.toString()),
              orElse: () {}),
        );
      });
    }

    return BlocBuilder<EncryptionCubit, EncryptionState>(
        builder: (context, EncryptionState state) {
      return SizedBox(
        width: 300,
        child: Column(
          children: [
            FormBuilder(
              key: _formKey,
              autovalidateMode: AutovalidateMode.onUserInteraction,
              child: Column(
                children: <Widget>[
                  FormBuilderTextField(
                    name: 'imap_host',
                    validator: FormBuilderValidators.required(context),
                    decoration: const InputDecoration(
                      labelText: 'Host',
                    ),
                  ),
                  FormBuilderTextField(
                    name: 'imap_userName',
                    validator: FormBuilderValidators.required(context),
                    decoration: const InputDecoration(
                      labelText: 'Username',
                    ),
                  ),
                  FormBuilderTextField(
                    name: 'imap_password',
                    validator: FormBuilderValidators.required(context),
                    decoration: const InputDecoration(
                      labelText: 'Password',
                    ),
                  ),
                  FormBuilderTextField(
                    name: 'imap_port',
                    initialValue: '993',
                    validator: FormBuilderValidators.integer(context),
                    decoration: const InputDecoration(
                      labelText: 'Port',
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: TextButton(
                      style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            vertical: 16.0,
                            horizontal: 32.0,
                          ),
                          backgroundColor: Colors.white),
                      onPressed: () {
                        _formKey.currentState!.save();
                        if (_formKey.currentState!.validate()) {
                          final formData = _formKey.currentState?.value;
                          ImapConfig cfg = ImapConfig(
                            host: formData!['imap_host'],
                            userName: formData['imap_userName'],
                            password: formData['imap_password'],
                            port: int.parse(formData['imap_port']),
                          );
                          context.read<EncryptionCubit>().setImapConfig(cfg);
                        }
                      },
                      child: Text(
                        'Save IMAP Config',
                        style: TextStyle(
                          color: AppColors.headerBgColor,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                  Center(
                    child: state.maybeWhen(
                        (sharedKey, imapConfig) =>
                            StatusTextWidget(imapConfig.toString()),
                        orElse: () {}),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    });
  }
}
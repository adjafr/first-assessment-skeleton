import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let server
let mostRecentCommand = ''
// let commandText = ''

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [server] = "localhost"') //AJ added [server] to give the optional command to load a server
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    server = connect({ host: args.server, port: 8080 }, () => {  //aj replaced localhost with args.server to connect to server of choosing
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })



  .action(function (input, callback) {
    let [ command, ...rest ] = words(input, /[^\s ]+/g)  //added lodash expression from https://regex101.com/#javascript so all characters but white spaces work
    let contents = '`<' + username + '> ' + '(' + command + ')' + ' ' + rest.join(' ') + '`'  //aj edited for printout

    if (command === 'disconnect') {
      server.end(new Message({ username, command }).toJSON() + '\n')

    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
      mostRecentCommand = command
      // commandText = mostRecentCommand

    } else if (command === 'broadcast') { //added by AJ
      contents = '`<' + username + '> ' + '(all) ' + '' + rest.join(' ') + '`'
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      mostRecentCommand = command

      // commandText = "all"

    } else if (command.charAt(0) ===  '@') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      // commandText = command

    } else if (command === 'users') { //added by AJ
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ
      // commandText = command

    } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
         && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand === 'broadcast') {
      command = mostRecentCommand
      contents = '`<' + username + '> ' + '(all) ' + contents + '`'   //+ ' ' + '' + rest.join(' ') +
     //  commandText = command
    //  contents = mostRecentCommand + ' ' + contents
      server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ

     } else if (command !== 'connect' && command !== 'disconnect' && command !== 'echo' && command !== 'broadcast'
          && command.charAt(0) !==  '@' && command !== 'users' && mostRecentCommand !== '') {
       command = mostRecentCommand
       contents = '`<' + username + '> ' + command + ' ' + contents // + rest.join(' ') + '`'
      //  commandText = command
      //  contents = mostRecentCommand + ' ' + contents
       server.write(new Message({ username, command, contents }).toJSON() + '\n') //added by AJ


    } else {
      this.log(`Command <${command}> was not recognized`)
    }



    callback()
  })
